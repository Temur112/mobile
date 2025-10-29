package zoro.benojir.callrecorder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callId: String,                  // unique UUID per call
    val phoneNumber: String,
    val callType: String,                // inbound / outbound / internal
    val callStatus: String,              // answered / no_answer / busy / failed
    val duration: Long,                  // in seconds
    val startTime: Long,
    val endTime: Long,
    val filePath: String,
    val synced: Boolean = false
)
