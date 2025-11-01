package zoro.benojir.callrecorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CallRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecord(record: CallRecordEntity): Long

    @Query("SELECT * FROM call_records WHERE synced = 0")
    suspend fun getUnsyncedRecords(): List<CallRecordEntity>

    @Query("UPDATE call_records SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("SELECT * FROM call_records ORDER BY startTime DESC")
    suspend fun getAll(): List<CallRecordEntity>


    @Query("SELECT * FROM call_records WHERE startTime < :threshold")
    suspend fun getOlderThan(threshold: Long): List<CallRecordEntity>

    @Query("DELETE FROM call_records WHERE id = :id")
    suspend fun deleteById(id: Long)

}
