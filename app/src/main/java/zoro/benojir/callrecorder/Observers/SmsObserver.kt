package zoro.benojir.callrecorder.observers

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.TelephonyManager
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
        Log.i("SmsObserver", "onChange triggered")

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
                if (id != lastSmsId) {
                    lastSmsId = id
                    val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                    val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))

                    val (sender, receiver) = resolveSenderReceiver(context, address, type)

                    Log.d("SMS_OBSERVER", "New SMS detected: sender=$sender, receiver=$receiver, body=$body")

                    // ✅ Save to local Room database
                    insertSmsToDb(context, sender, receiver, body)

                    // ✅ Still enqueue for upload
                    SmsUploadHelper.enqueueSmsUpload(context, sender, body)
                }
            }
        }
    }

    private fun insertSmsToDb(context: Context, sender: String, receiver: String, body: String) {
        val db = AppDatabase.getInstance(context)
        val smsDao = db.smsDao()

        CoroutineScope(Dispatchers.IO).launch {
            val sms = SmsEntity(
                sender = sender,
                receiver = receiver,
                text = body,
                timestamp = System.currentTimeMillis(),
                synced = false
            )
            smsDao.insertSms(sms)
            Log.d("SMS_DB", "Inserted into DB: $sender -> $receiver | $body")
        }
    }

    /**
     * Resolves who is sender and who is receiver based on SMS type:
     * - Inbox (1): someone else sent to us → sender = address, receiver = device number
     * - Sent (2): we sent → sender = device number, receiver = address
     */
    private fun resolveSenderReceiver(context: Context, address: String, type: Int): Pair<String, String> {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simNumber = tm.line1Number ?: "unknown"

        return if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
            Pair(address, simNumber)
        } else {
            Pair(simNumber, address)
        }
    }
}
