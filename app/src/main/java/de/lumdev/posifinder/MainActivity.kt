package de.lumdev.posifinder

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import de.lumdev.posifinder.databinding.ActivityMainBinding
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


val FETCH_METHOD_DEFAULT: String = "getCurrent()" //attention: use proper string from strings.xml
var fetchMethod: String = FETCH_METHOD_DEFAULT
var fetchType: LocationFetchType = LocationFetchType.GET_CURRENT_LOCATION

enum class LocationFetchType {
    GET_LAST_LOCATION, GET_CURRENT_LOCATION, REQUEST_LOCATION_UPDATES, NONE
}

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val STARTED_NORMALLY: String = "STARTED_NORMALLY"
    private val STARTED_FROM_INTENT_SAVE_POSI: String = "STARTED_FROM_INTENT_SAVE_POSI"
    private val STARTED_FROM_INTENT_SAVE_POSI_2: String = "STARTED_FROM_INTENT_SAVE_POSI_2"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val sharedPref = getSharedPreferences(getString(R.string.pref_file), Context.MODE_PRIVATE)

        var suppress_ui_notifications: Boolean = true


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)


        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.
                    if (!suppress_ui_notifications) Toast.makeText(this, "Precise Location granted.", Toast.LENGTH_SHORT).show()
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                    if (!suppress_ui_notifications) Toast.makeText(this, "Only Approximate Location granted.", Toast.LENGTH_SHORT).show()
                } else -> {
                // No location access granted.
                if (!suppress_ui_notifications) Toast.makeText(this, "No Location Access granted.", Toast.LENGTH_SHORT).show()
            }
            }
        }
        // ...
        // Before you perform the actual permission request, check whether your app
        // already has the permissions, and whether your app needs to show a permission
        // rationale dialog. For more details, see Request permissions.
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))

        if (sharedPref != null) fetchMethod = sharedPref.getString(getString(R.string.pref_id_fetch_method), FETCH_METHOD_DEFAULT).toString()
        fetchType = when (fetchMethod){
            getString(R.string.btn_fetch_method_getLast) -> LocationFetchType.GET_LAST_LOCATION
            getString(R.string.btn_fetch_method_getCurrent) -> LocationFetchType.GET_CURRENT_LOCATION
            getString(R.string.btn_fetch_method_request_updates) -> LocationFetchType.REQUEST_LOCATION_UPDATES
            else -> LocationFetchType.NONE
        }
        val fetchTypeRetryIfNull = LocationFetchType.NONE
//        println("FetchMethod selected: $fetchMethod")

        //FAB onClick Listener
        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
            fetch_location(ui_notifications = (!suppress_ui_notifications), fetchType = fetchType, fetchTypeRetryIfNull = fetchTypeRetryIfNull)
        }

        var activity_starting_method: String = STARTED_NORMALLY

        //set to Background if opening from intent
        if (intent.getStringExtra(getString(R.string.EXTRA_START_ACTIVITY_IN_BACKGROUND)) == getString(R.string.EXTRA_START_ACTIVITY_IN_BACKGROUND)){
            activity_starting_method = STARTED_FROM_INTENT_SAVE_POSI
            println("----------- pls put activity to background now-----------")
//            suppress_ui_notifications = true
            fetch_location(ui_notifications = (!suppress_ui_notifications), fetchType = fetchType, fetchTypeRetryIfNull = fetchTypeRetryIfNull)
            //invoke service for location fetching
//            val intentServiceIntent = Intent(this, ShortcutIntentService::class.java)
//            intentServiceIntent.setAction(ACTION_FETCH_LOCATION)
//            startService(intentServiceIntent)

            //move task to background
            moveTaskToBack(true)
        }

        //set to Background if opening from intent
        if (intent.getStringExtra(getString(R.string.EXTRA_START_ACTIVITY_IN_BACKGROUND_2)) == getString(R.string.EXTRA_START_ACTIVITY_IN_BACKGROUND_2)){
            activity_starting_method = STARTED_FROM_INTENT_SAVE_POSI_2
            println("----------- pls leave activity in foreground -----------")
//            suppress_ui_notifications = true
            fetch_location(ui_notifications = (!suppress_ui_notifications), fetchType = fetchType, fetchTypeRetryIfNull = fetchTypeRetryIfNull)
            //invoke service for location fetching
//            val intentServiceIntent = Intent(this, ShortcutIntentService::class.java)
//            intentServiceIntent.setAction(ACTION_FETCH_LOCATION)
//            startService(intentServiceIntent)

            //keep activity in foreground (don't move to back!!)
        }

        //----set starting time depending on starting method-----
        //get current DateTime
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy (HH:mm:ss)")
        val currentDateTime = LocalDateTime.now().format(formatter)
//        val pref_id_write_date_to: String
        val pref_id_write_date_to: String = when (activity_starting_method){
            STARTED_NORMALLY -> getString(R.string.pref_id_last_used_posifinder)
            STARTED_FROM_INTENT_SAVE_POSI -> getString(R.string.pref_id_last_used_save_position)
            STARTED_FROM_INTENT_SAVE_POSI_2 -> getString(R.string.pref_id_last_used_save_position_2)
            else -> getString(R.string.pref_id_last_used_posifinder)
        }
        //write date to shared preferences
        with (sharedPref.edit()) {
            putString(pref_id_write_date_to, currentDateTime)
            apply()
        }
        //write TextView Content depending on sharedPreferences
        //--> in appropriate fragment file

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        //this is not needed anymore, cause navigation is handled by NavigationController (important: ids of menu-items and nav_graph destinations must be identical
//        return when (item.itemId) {
//            R.id.action_settings -> true
//            R.id.action_homeFragment_to_aboutAppFragment -> findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_homeFragment_to_aboutAppFragment) return true
//            else -> super.onOptionsItemSelected(item)
//        }
        return onNavDestinationSelected(item, findNavController(R.id.nav_host_fragment_content_main)) || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    //fetch location + save it in preferences + update UI views
    private fun fetch_location(ui_notifications: Boolean, fetchType: LocationFetchType, fetchTypeRetryIfNull: LocationFetchType){
        println("FetchMethod selected: $fetchMethod")
        if (ui_notifications) Toast.makeText(this, "Fetching exact location. This may take a little time.", Toast.LENGTH_LONG).show()

        val sharedPref = getSharedPreferences(getString(R.string.pref_file), Context.MODE_PRIVATE)
        var lat: Double = 0.0
        var lon: Double = 0.0
        var time: Long = 0L

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            if (ui_notifications) Toast.makeText(this, "No Location Access granted.", Toast.LENGTH_LONG).show()
        }else {

            var locationTask: Task<Location>? = null
            if (fetchType == LocationFetchType.GET_LAST_LOCATION) locationTask = fusedLocationClient.lastLocation
            else if (fetchType == LocationFetchType.GET_CURRENT_LOCATION) locationTask = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,CancellationTokenSource().token)
            locationTask?.addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations this can be null
                if (location != null) {
                    val text: String =
                        "Your Location is: " + location.latitude.toString() + ", " + location.longitude.toString()
                    println(text)
                    if (ui_notifications) Toast.makeText(this, text, Toast.LENGTH_LONG).show()

                    lat = location.latitude
                    lon = location.longitude
                    time = location.time
                    //save new location values (to shared preferences and update UI)
                    if (lat != 0.0 && lon != 0.0 && time != 0L) saveLocation(lat, lon, time)

                } else {
                    println("location = null ------------ bad :-(")
        //                        if (ui_notifications) Toast.makeText(this, "Fetched Location == NULL", Toast.LENGTH_LONG).show()
                    Toast.makeText(this, "Fetched Location == NULL", Toast.LENGTH_LONG).show()

                    when (fetchTypeRetryIfNull) {
                        LocationFetchType.GET_LAST_LOCATION -> fetch_location(
                            ui_notifications,
                            fetchTypeRetryIfNull,
                            LocationFetchType.NONE
                        )

                        LocationFetchType.GET_CURRENT_LOCATION -> fetch_location(
                            ui_notifications,
                            fetchTypeRetryIfNull,
                            LocationFetchType.NONE
                        )

                        else -> return@addOnSuccessListener
                    }

                }
            }
            //end first part of if selection for fetchType
            if (fetchType == LocationFetchType.REQUEST_LOCATION_UPDATES){

//                println("Go for Requesting Location Updates.")

                locationCallback = object : LocationCallback() {
                    override fun onLocationAvailability(availability: LocationAvailability) {
//                        super.onLocationAvailability(po)
                        println("Location Availability: ${availability.isLocationAvailable}")
                    }
                    override fun onLocationResult(locationResult: LocationResult) {
//                        println("Inside LocationResult Callback..........")
//                        for (location in locationResult.locations){
//                            // Update UI with location data
//                            // ...
//                            println("Location from RequestLocationUpdates: ${location.latitude}, ${location.longitude}")
//                        }
                        lat = locationResult.lastLocation!!.latitude
                        lon = locationResult.lastLocation!!.longitude
                        time = locationResult.lastLocation!!.time
                        println("Location from RequestLocationUpdates: ${locationResult.lastLocation?.latitude}, ${locationResult.lastLocation?.longitude}")
                    }
                }
//
                println("Starting location updates now.")
                startLocationUpdates()

                Handler(Looper.getMainLooper()).postDelayed({
                    println("Stopping Location updates now.")
                    stopLocationUpdates()
                    //saving obtained values to Preferences and updating UI
                    println("Final Location from RequestLocationUpdates: $lat, $lon - at $time")
                    if (lat != 0.0 && lon != 0.0 && time != 0L) saveLocation(lat, lon, time)
                }, 20000)

            }

        }
    }

    private lateinit var locationCallback: LocationCallback

    private fun startLocationUpdates() {
        //create location request builder
        val locationRequest = LocationRequest.Builder(500)
//                    .setIntervalMillis(10000)
//                    .setFastestIntervalMillis(5000)
//                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
        //check for location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) { //--> permissions are not granted
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

//    override fun onPause() {
//        super.onPause()
//        stopLocationUpdates()
//    }
//    override fun onResume() {
//        super.onResume()
//        if (requestingLocationUpdates) startLocationUpdates()
//    }



    //from https://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
    fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(double))

    fun SharedPreferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))



    fun saveLocation (lat: Double, lon: Double, timeStampEpochMillies: Long){

        val sharedPref = getSharedPreferences(getString(R.string.pref_file), Context.MODE_PRIVATE)

        val instant = Instant.ofEpochMilli(timeStampEpochMillies)
        val zoneId = ZoneId.systemDefault() // Use the system default time zone
        val localDateTime = instant.atZone(zoneId).toLocalDateTime()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy (HH:mm:ss)")
        val currentDateTime = localDateTime.format(formatter)
        val titleText = "Position on $currentDateTime"

        //save location values to preferences
        with(sharedPref.edit()) {
            putDouble(getString(de.lumdev.posifinder.R.string.pref_id_latitude),lat)
            putDouble(getString(de.lumdev.posifinder.R.string.pref_id_longitude),lon)
            putString(getString(de.lumdev.posifinder.R.string.pref_id_title),titleText)
            apply()
        }
        //debug print location values
        //                            println(sharedPref.getDouble(getString(R.string.pref_id_latitude),10000.0))
        //                            println(sharedPref.getDouble(getString(R.string.pref_id_longitude),20000.0))

        //write location values to UI
        findViewById<TextView>(R.id.latitudeTextView).text = lat.toString()
        findViewById<TextView>(R.id.longitudeTextView).text = lon.toString()
        findViewById<TextView>(R.id.titleTextView).text = titleText
    }
}