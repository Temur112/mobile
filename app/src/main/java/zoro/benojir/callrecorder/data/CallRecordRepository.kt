package zoro.benojir.callrecorder.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class CallRecordRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).callRecordDao()

//    suspend fun insert(record: CallRecordEntity): Long = dao.insertCallRecord(record)
    suspend fun getUnsynced() = dao.getUnsyncedRecords()
    suspend fun markSynced(id: Long) = dao.markAsSynced(id)
    fun insert(record: CallRecordEntity): Long {
        return runBlocking {
            dao.insertCallRecord(record)
        }
    }
    fun insertBlocking(record: CallRecordEntity): Long {
        return kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            dao.insertCallRecord(record)
        }
    }

}
