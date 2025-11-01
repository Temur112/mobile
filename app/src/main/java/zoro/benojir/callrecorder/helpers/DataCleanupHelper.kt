package zoro.benojir.callrecorder.helpers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zoro.benojir.callrecorder.data.AppDatabase
import java.io.File

object DataCleanupHelper {

    private const val SMS_RETENTION_DAYS = 30
    private const val CALL_RETENTION_DAYS = 60

    suspend fun cleanOldData(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val now = System.currentTimeMillis()

                val smsThreshold = now - SMS_RETENTION_DAYS * 24 * 60 * 60 * 1000L
                val callThreshold = now - CALL_RETENTION_DAYS * 24 * 60 * 60 * 1000L

                // 🧹 Delete old SMS messages
                val deletedSms = db.smsDao().deleteOlderThan(smsThreshold)
                Log.i("Cleanup", "Deleted $deletedSms old SMS entries.")

                // 🧹 Delete old Call Records
                val oldCalls = db.callRecordDao().getOlderThan(callThreshold)
                var deletedCallCount = 0

                for (call in oldCalls) {
                    // Delete file if it exists
                    if (call.filePath.isNotEmpty()) {
                        val file = File(call.filePath)
                        if (file.exists()) file.delete()
                    }
                    db.callRecordDao().deleteById(call.id)
                    deletedCallCount++
                }
                Log.i("Cleanup", "Deleted $deletedCallCount old calls + files.")

            } catch (e: Exception) {
                Log.e("Cleanup", "Error cleaning old data: ", e)
            }
        }
    }
}
