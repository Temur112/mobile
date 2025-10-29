package zoro.benojir.callrecorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CallRecordDao {
    @Insert
    suspend fun insertCallRecord(record: CallRecordEntity): Long

    @Query("SELECT * FROM call_records WHERE synced = 0")
    suspend fun getUnsyncedRecords(): List<CallRecordEntity>

    @Query("UPDATE call_records SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
}
