package de.lumdev.posifinder

import android.Manifest
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// TODO: Rename actions, choose action names that describe tasks that this
// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
const val ACTION_FETCH_LOCATION = "de.lumdev.posifinder.action.fetch_location"
const val ACTION_BAZ = "de.lumdev.posifinder.action.BAZ"

//// TODO: Rename parameters
//const val EXTRA_PARAM1 = "de.lumdev.posifinder.extra.PARAM1"
//const val EXTRA_PARAM2 = "de.lumdev.posifinder.extra.PARAM2"

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class ShortcutIntentService : IntentService("ShortcutIntentService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_FETCH_LOCATION -> {
                println("Service started after ACTION_FETCH_LOCATION -------------------------------------")
                fetch_location()
            }

//            ACTION_BAZ -> {
//                val param1 = intent.getStringExtra(EXTRA_PARAM1)
//                val param2 = intent.getStringExtra(EXTRA_PARAM2)
//                handleActionBaz(param1, param2)
//            }
        }

//        println("Service started without specific ACTION-------------------------------------")
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionFoo(param1: String?, param2: String?) {
        TODO("Handle action Foo")
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionBaz(param1: String?, param2: String?) {
        TODO("Handle action Baz")
    }

    private fun fetch_location(){
        Toast.makeText(this, "Fetching exact location. This may take a little time.", Toast.LENGTH_LONG).show()

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        val sharedPref = getSharedPreferences(getString(R.string.pref_file), Context.MODE_PRIVATE)

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
            Toast.makeText(this, "No Location Access granted.", Toast.LENGTH_SHORT).show()
        }else {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location: Location? ->
                    // Got last known location. In some rare situations this can be null
                    if (location != null) {
                        val text : String = "Your Location is: "+location.latitude.toString() + ", "+ location.longitude.toString()
                        println(text)
//                        Toast.makeText(this, text, Toast.LENGTH_LONG).show()

                        //get current DateTime
                        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy (HH:mm)")
                        val currentDateTime = LocalDateTime.now().format(formatter)
                        val titleText = "Position on $currentDateTime"

                        //save location values to preferences
                        with (sharedPref.edit()) {
                            putDouble(getString(de.lumdev.posifinder.R.string.pref_id_latitude), location.latitude)
                            putDouble(getString(de.lumdev.posifinder.R.string.pref_id_longitude), location.longitude)
                            putString(getString(de.lumdev.posifinder.R.string.pref_id_title), titleText)
                            apply()
                            println("ShortcutIntentService saved new location to preferences.")
                        }
                        //debug print location values
//                            println(sharedPref.getDouble(getString(R.string.pref_id_latitude),10000.0))
//                            println(sharedPref.getDouble(getString(R.string.pref_id_longitude),20000.0))

                        Toast.makeText(this, "ShotcutIntentService saved new Location to preferences.", Toast.LENGTH_LONG).show()

                    } else {
                        println("location = null ------------ bad :-(")
                        Toast.makeText(this, "Fetched Location == NULL", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    override fun onTimeout(startId: Int) {
        super.onTimeout(startId)
        println("ShortcutIntentService onTimeout() called.")
    }

    //from https://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
    fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(double))

    fun SharedPreferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))
}