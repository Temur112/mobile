package zoro.benojir.callrecorder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import zoro.benojir.callrecorder.receivers.SmsReceiver



@Entity(tableName = "sms")
data class SmsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val receiver: String,
    val text: String,
    val timestamp: Long,
    val synced: Boolean = false,
    val status: String
)



