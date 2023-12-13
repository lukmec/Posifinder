package de.lumdev.posifinder

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ShortcutService : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //debug print
        println("ShortcutService.kt started -------------------------------------")

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        println("ShortcutService.kt is stopped -------------------------------------")
    }

    override fun onBind(intent: Intent): IBinder? {
//        TODO("Return the communication channel to the service.")
        return null
    }
}