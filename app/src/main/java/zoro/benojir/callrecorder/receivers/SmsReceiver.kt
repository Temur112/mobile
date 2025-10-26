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
        Log.d("PPP", "onReceive: Broadcast received")

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val bundle: Bundle? = intent.extras
            if (bundle != null) {
                val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (msg in msgs) {
                    val sender = msg.displayOriginatingAddress ?: "unknown"
                    val text = msg.displayMessageBody ?: ""

                    Log.d("SMSTTT", "onReceive: $text from $sender")

                    val smsEntity = SmsEntity(
                        sender = sender ?: "unknown",
                        receiver = "me",
                        text = text,
                        timestamp = System.currentTimeMillis(),
                        synced = false
                    )

                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = AppDatabase.getInstance(context).smsDao()
                        dao.insertSms(smsEntity)
                    }


                    // "me" is the receiver since this is an incoming SMS
                    SmsUploadHelper.enqueueSmsUpload(context, sender, "me", text)
                }
            }
        }
    }
}
