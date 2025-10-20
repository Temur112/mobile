package zoro.benojir.callrecorder.helpers

import android.content.Context
import androidx.work.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SmsUploadHelper {
    companion object {
        fun enqueueSmsUpload(context: Context, sender: String, text: String) {
            val data = workDataOf("sender" to sender, "text" to text)
            val work = OneTimeWorkRequestBuilder<SmsUploadWorker>()
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
}

class SmsUploadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val sender = inputData.getString("sender") ?: return Result.failure()
        val text = inputData.getString("text") ?: return Result.failure()

        return try {
            val client = OkHttpClient()
            val formBody = FormBody.Builder()
                .add("sender", sender)
                .add("text", text)
                .build()

            val request = Request.Builder()
                .url("http://192.168.233.53:9232/sms")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success() else Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
