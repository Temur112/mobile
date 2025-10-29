package zoro.benojir.callrecorder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import zoro.benojir.callrecorder.data.AppDatabase
import zoro.benojir.callrecorder.data.SmsEntity
import zoro.benojir.callrecorder.helpers.SmsUploadHelper

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            Log.d("SmsReceiver", "Incoming SMS detected")

            val bundle: Bundle? = intent.extras
            if (bundle != null) {
                val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (msg in msgs) {
                    val sender = msg.displayOriginatingAddress ?: "unknown"
                    val text = msg.displayMessageBody ?: ""

                    Log.d("SMSTTT", "Incoming: $text from $sender")

                    val smsEntity = SmsEntity(
                        sender = sender,
                        receiver = "me",
                        text = text,
                        timestamp = System.currentTimeMillis(),
                        synced = false,
                        status = "received"
                    )

                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = AppDatabase.getInstance(context).smsDao()
                        dao.insertSms(smsEntity)
                    }

                    SmsUploadHelper.enqueueSmsUpload(context, sender, "me", text)
                }
            }
        }
    }
}
