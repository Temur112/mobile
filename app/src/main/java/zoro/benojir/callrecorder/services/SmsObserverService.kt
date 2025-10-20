package zoro.benojir.callrecorder.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import zoro.benojir.callrecorder.observers.SmsObserver

class SmsObserverService : Service() {

    private lateinit var inboxObserver: SmsObserver
    private lateinit var sentObserver: SmsObserver

    // It's good practice to create the handler once.
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        val inboxUri = Uri.parse("content://sms/inbox")
        val sentUri = Uri.parse("content://sms/sent")

        // Pass the context, uri and the handler to the observer's constructor.
        inboxObserver = SmsObserver(this, inboxUri, handler)
        sentObserver = SmsObserver(this, sentUri, handler)

        // The method signature for registerContentObserver is correct.
        contentResolver.registerContentObserver(
            inboxUri,
            true,
            inboxObserver
        )

        contentResolver.registerContentObserver(
            sentUri,
            true,
            sentObserver
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(inboxObserver)
        contentResolver.unregisterContentObserver(sentObserver)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
