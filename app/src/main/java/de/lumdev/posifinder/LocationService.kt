package de.lumdev.posifinder

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import de.lumdev.posifinder.MyLocationClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LocationService: Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var  myLocationClient: MyLocationClient
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        myLocationClient = MyDefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
        println("LocationService onCreate()")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start(){
        println("LocationService started.")

        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Saving Current Location")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.baseline_my_location_24)
            .setOngoing(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        myLocationClient.getLocationUpdates(10L)
            .catch { e -> e.printStackTrace() }
            .onEach {
                val lat = it.latitude.toString()
                val long = it.longitude.toString()
                println("LocationService - Location: $lat, $long")
                saveLocation(it.latitude, it.longitude)
                val updateNotification = notification.setContentText("Location: ($lat, $long)")
                notificationManager.notify(1, updateNotification.build())
                //stop self after first location is saved
                stop()
            }
            .launchIn(serviceScope)

        startForeground(1, notification.build())
    }

    private fun stop(){
        println("LocationService stopped.")

        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        println("LocationService onDestroy()")
        serviceScope.cancel()
    }

    //from https://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
    fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(double))

    fun SharedPreferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))



    fun saveLocation (lat: Double, lon: Double){

        val sharedPref = getSharedPreferences(getString(R.string.pref_file), Context.MODE_PRIVATE)

        //get current DateTime
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy (HH:mm:ss)")
        val currentDateTime = LocalDateTime.now().format(formatter)
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

    }

    companion object{
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}