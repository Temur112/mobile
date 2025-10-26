package zoro.benojir.callrecorder.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import zoro.benojir.callrecorder.observers.SmsObserver

class SmsObserverService : Service() {

    private lateinit var smsObserver: SmsObserver
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        val smsUri = Uri.parse("content://sms")
        smsObserver = SmsObserver(this, smsUri, handler)

        contentResolver.registerContentObserver(smsUri, true, smsObserver)
        Log.i("SmsObserverService", "Observing all SMS (inbox + sent)")
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(smsObserver)
        Log.i("SmsObserverService", "Observer stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

