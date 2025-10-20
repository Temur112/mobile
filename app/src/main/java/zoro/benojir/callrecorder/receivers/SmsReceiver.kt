package zoro.benojir.callrecorder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsMessage
import zoro.benojir.callrecorder.helpers.SmsUploadHelper

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val bundle: Bundle? = intent.extras
            if (bundle != null) {
                val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (msg in msgs) {
                    val sender = msg.displayOriginatingAddress ?: "unknown"
                    val text = msg.displayMessageBody ?: ""
                    SmsUploadHelper.enqueueSmsUpload(context, sender, text)
                }
            }
        }
    }
}
