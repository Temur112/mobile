package zoro.benojir.callrecorder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import zoro.benojir.callrecorder.helpers.SmsUploadHelper

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PPP", "onReceive: Broadcast recieved ")
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val bundle: Bundle? = intent.extras
            if (bundle != null) {
                val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (msg in msgs) {
                    val sender = msg.displayOriginatingAddress ?: "unknown"
                    val text = msg.displayMessageBody ?: ""
                    Log.d("SMSTTT", "onReceive: $text")
                    SmsUploadHelper.enqueueSmsUpload(context, sender, text)
                }
            }
        }
    }
}
