package zoro.benojir.callrecorder.helpers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File

class UploadRecordingWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val filePath = inputData.getString("file_path") ?: return Result.failure()
        val file = File(filePath)
        if (!file.exists()) return Result.failure()

        return try {
            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    RequestBody.create("audio/m4a".toMediaTypeOrNull(), file)
                )
                .build()

            val request = Request.Builder()
                .url("http://192.168.233.53:9232/voice")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success() else Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
