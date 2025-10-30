package zoro.benojir.callrecorder.receivers

import android.R
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
import zoro.benojir.callrecorder.helpers.CustomFunctions
import zoro.benojir.callrecorder.helpers.DeviceInfoHelper
import zoro.benojir.callrecorder.helpers.SmsUploadHelper

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            Log.d("SmsReceiver", "Incoming SMS detected")

            val bundle: Bundle? = intent.extras
            val username = CustomFunctions.getUserName(context)
            if (bundle != null) {
                val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (msg in msgs) {
                    val sender = msg.displayOriginatingAddress ?: "unknown"
                    val text = msg.displayMessageBody ?: ""
                    var receiver = DeviceInfoHelper.getOwnPhoneNumber(context)

                    Log.d("SMSTTT", "Incoming: $text to $receiver")

                    Log.d("SMSTTT", "Incoming: $text from $sender")

                    if (receiver == "unknown") {receiver = "me"}

                    val smsEntity = SmsEntity(
                        sender = sender,
                        receiver = receiver,
                        text = text,
                        timestamp = System.currentTimeMillis(),
                        synced = false,
                        status = "received",
                        username = username
                    )
                    Log.d("SMSTTT", "inserting and uploading sms")
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = AppDatabase.getInstance(context).smsDao()
                        val smsId = dao.insertSms(smsEntity)
                        SmsUploadHelper.enqueueSmsUpload(context, sender=sender, receiver=receiver, text=text, smsid = smsId.toString(), action = "receive", username = username, status="received", direction="inbound")
                    }


                }
            }
        }
    }
}
