package zoro.benojir.callrecorder.observers

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
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
    handler: Handler
) : ContentObserver(handler) {

    private var lastSmsId: Long = -1

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)

        val cursor = context.contentResolver.query(
            uri,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.TYPE
            ),
            null, null,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                if (id == lastSmsId) return
                lastSmsId = id

                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: "unknown"
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))

                var sender: String
                var receiver: String
                var status: String

                when (type) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> {
                        // Skip incoming — SmsReceiver already handles it
                        Log.d("SMSTTT", "Skipping inbox message in observer to avoid duplication")
                        return
                    }

                    Telephony.Sms.MESSAGE_TYPE_SENT -> {
                        // Handle outgoing SMS
                        sender = "me"
                        receiver = address
                        status = "sent"
                    }

                    Telephony.Sms.MESSAGE_TYPE_FAILED -> {
                        sender = "me"
                        receiver = address
                        status = "failed"
                    }

                    else -> return // Ignore drafts, outbox, failed, etc.
                }

                Log.d("SMSTTT", "onChange: $body from=$sender to=$receiver (id=$id, type=$type)")

                val smsEntity = SmsEntity(
                    sender = sender,
                    receiver = receiver,
                    text = body,
                    timestamp = System.currentTimeMillis(),
                    synced = false,
                    status = status,
                    username = "unknown"
                )

                CoroutineScope(Dispatchers.IO).launch {
                    val dao = AppDatabase.getInstance(context).smsDao()
                    dao.insertSms(smsEntity)
                }

                SmsUploadHelper.enqueueSmsUpload(context, sender, receiver, body, )
            }
        }
    }
}

