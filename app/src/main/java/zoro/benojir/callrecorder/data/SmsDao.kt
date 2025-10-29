package zoro.benojir.callrecorder.data

import androidx.room.*

@Dao
interface SmsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSms(sms: SmsEntity): Long

    @Query("SELECT * FROM sms ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllSms(limit: Int, offset: Int): List<SmsEntity>

    @Query("SELECT * FROM sms WHERE synced = 0")
    suspend fun getUnsyncedSms(): List<SmsEntity>

    @Query("UPDATE sms SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("UPDATE sms SET synced = :isSynced WHERE receiver = :receiver AND text = :body")
    suspend fun updateSyncedStatus(receiver: String, body: String, isSynced: Boolean)
}
