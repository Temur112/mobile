package zoro.benojir.callrecorder.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import zoro.benojir.callrecorder.helpers.DataCleanupHelper

class DataCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // Run cleanup logic
            DataCleanupHelper.cleanOldData(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry() // try again if something failed
        }
    }
}
