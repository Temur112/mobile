package zoro.benojir.callrecorder.observers

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import zoro.benojir.callrecorder.data.AppDatabase
import zoro.benojir.callrecorder.data.SmsEntity
import zoro.benojir.callrecorder.helpers.SmsUploadHelper

class SmsObserver(
    private val context: Context,
    private val uri: Uri,
    handler: Handler = Handler(Looper.getMainLooper())
) : ContentObserver(handler) {

    private var lastSmsId: Long = -1

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        Log.i("SmsObserver", "onChange: ")
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.TYPE),
            null, null, Telephony.Sms.DEFAULT_SORT_ORDER
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                if (id != lastSmsId) {
                    lastSmsId = id
                    val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                    val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))

                    // Determine who is sender and receiver
                    val (sender, receiver) = if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                        Pair(address, "me") // Incoming message
                    } else {
                        Pair("me", address) // Outgoing message
                    }

                    Log.d("SMSTTT", "onChange: $body from=$sender to=$receiver")

                    val smsEntity = SmsEntity(
                        sender = "me",
                        receiver = address ?: "unknown",
                        text = body,
                        timestamp = System.currentTimeMillis(),
                        synced = false
                    )

                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = AppDatabase.getInstance(context).smsDao()
                        dao.insertSms(smsEntity)
                    }

                    SmsUploadHelper.enqueueSmsUpload(context, sender, receiver, body)
                }
            }
        }
    }
}
