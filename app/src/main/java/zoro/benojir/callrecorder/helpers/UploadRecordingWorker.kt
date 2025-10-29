package zoro.benojir.callrecorder.helpers

import android.content.Context
import java.util.concurrent.TimeUnit
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import kotlin.contracts.contract

class UploadRecordingWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val filePath = inputData.getString("file_path") ?: return Result.failure()
        val file = File(filePath)
        if (!file.exists()) return Result.failure()

        val token = CustomFunctions.getToken(applicationContext)
        var serverUrl = CustomFunctions.getServerUrl(applicationContext)

        if (token.isNullOrEmpty()) {
            Log.e("UploadRecordingWorker", "❌ Missing token, cannot upload voice file.")
            return Result.failure()
        }

        if (serverUrl.isNullOrEmpty()) {
            Log.e("UploadRecordingWorker", "❌ Missing server URL, cannot upload voice file.")
            return Result.failure()
        }

        if (!serverUrl.endsWith("/")) serverUrl += "/"

        val fullUrl = serverUrl + "voice"
        Log.d("UploadRecordingWorker", "🎤 Uploading to endpoint: $fullUrl")

        return try {
            val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    RequestBody.create("audio/m4a".toMediaTypeOrNull(), file)
                )
                .build()

            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseText = response.body?.string() ?: ""
            Log.d("UploadRecordingWorker", "📩 Response ${response.code}: $responseText")

            if (response.isSuccessful) {
                Log.i("UploadRecordingWorker", "✅ Voice uploaded successfully")
                Result.success()
            } else {
                Log.w("UploadRecordingWorker", "⚠️ Upload failed with code ${response.code}")
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
