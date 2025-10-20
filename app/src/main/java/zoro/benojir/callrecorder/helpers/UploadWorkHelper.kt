package zoro.benojir.callrecorder.helpers

import android.content.Context
import androidx.work.*
import java.io.File
import java.util.concurrent.TimeUnit

object UploadWorkHelper {

    fun enqueueVoiceUpload(context: Context, filePath: String) {
        val fileName = File(filePath).nameWithoutExtension
        val uniqueWorkName = "upload_$fileName" // ensures one upload per file

        val uploadRequest = OneTimeWorkRequestBuilder<UploadRecordingWorker>()
            .setInputData(workDataOf("file_path" to filePath))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10, TimeUnit.SECONDS
            )
            .build()

        // 👇 ensures only one upload per file (prevents duplicates)
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.KEEP, // or REPLACE if you want to retry a failed one
            uploadRequest
        )
    }
}
