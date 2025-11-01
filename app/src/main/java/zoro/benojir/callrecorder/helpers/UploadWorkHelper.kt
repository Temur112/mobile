package zoro.benojir.callrecorder.helpers

import android.content.Context
import android.util.Log
import androidx.work.*
import java.io.File
import java.util.concurrent.TimeUnit

object UploadWorkHelper {

    fun enqueueVoiceUpload(context: Context, filePath: String) {
        val fileName = if (filePath.isNotEmpty()) File(filePath).nameWithoutExtension else System.currentTimeMillis().toString()
        val uniqueWorkName = "upload_$fileName"
        Log.d("uppp", "Upload worker check")
        val uploadRequest = OneTimeWorkRequestBuilder<UploadRecordingWorker>()
            .setInputData(workDataOf("file_path" to filePath))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                10, TimeUnit.SECONDS
            )
            .build()
        Log.d("uppp", "Upload worker check after build")
        // 👇 ensures only one upload per file (prevents duplicates)
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.KEEP, // or REPLACE if you want to retry a failed one
            uploadRequest
        )
    }
}
