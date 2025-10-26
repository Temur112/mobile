package zoro.benojir.callrecorder.helpers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SmsUploadHelper {
    companion object {
        private var lastUploadedBody: String? = null
        private var lastUploadedReceiver: String? = null
        private var lastUploadTime: Long = 0

        fun enqueueSmsUpload(context: Context, sender: String, receiver: String, text: String) {
            val now = System.currentTimeMillis()

            // ✅ Prevent duplicate uploads within 5 seconds for the same message
            if (text == lastUploadedBody &&
                receiver == lastUploadedReceiver &&
                now - lastUploadTime < 5000
            ) {
                Log.w("SmsUploadHelper", "⚠️ Duplicate SMS detected — skipping upload ($receiver: $text)")
                return
            }

            lastUploadedBody = text
            lastUploadedReceiver = receiver
            lastUploadTime = now

            Log.d("SmsUploadHelper", "📤 Enqueueing SMS upload | sender=$sender, receiver=$receiver, text=$text")

            val data = workDataOf(
                "sender" to sender,
                "receiver" to receiver,
                "text" to text
            )

            val work = androidx.work.OneTimeWorkRequestBuilder<SmsUploadWorker>()
                .setInputData(data)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.LINEAR,
                    15,
                    TimeUnit.SECONDS
                )
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(work)
        }
    }
}

class SmsUploadWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sender = inputData.getString("sender") ?: return@withContext Result.failure()
        val receiver = inputData.getString("receiver") ?: return@withContext Result.failure()
        val body = inputData.getString("text") ?: return@withContext Result.failure()

        Log.d("SmsUploadWorker", "⚙️ Worker started execution")
        Log.d("SmsUploadWorker", "📦 Input data -> sender=$sender | receiver=$receiver | body=$body")

        try {
            val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("auth_token", "") ?: ""
            Log.d("SmsUploadWorker", "🔑 Loaded token: ${if (token.isNotEmpty()) "present (${token.take(10)}...)" else "missing"}")

            if (token.isEmpty()) {
                Log.e("SmsUploadWorker", "❌ No token found. Cannot upload SMS.")
                return@withContext Result.failure()
            }

            // ✅ Build JSON payload
            val json = JSONObject().apply {
                put("sender", sender)
                put("receiver", receiver)
                put("body", body)
            }

            val jsonStr = json.toString()
            Log.d("SmsUploadWorker", "🧾 JSON body -> $jsonStr")

            val requestBody = jsonStr.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("http://192.168.233.53:9232/sms")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d("SmsUploadWorker", "🌐 Sending POST request to http://192.168.233.53:9232/sms")

            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                Log.d("SmsUploadWorker", "📩 Response code=${response.code}, body=$responseBody")

                return@withContext when {
                    response.isSuccessful -> {
                        Log.i("SmsUploadWorker", "✅ SMS uploaded successfully")
                        Result.success()
                    }
                    response.code == 401 -> {
                        Log.e("SmsUploadWorker", "❌ Unauthorized — invalid or expired token")
                        Result.failure()
                    }
                    else -> {
                        Log.w("SmsUploadWorker", "⚠️ Upload failed with code ${response.code}")
                        Result.retry()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsUploadWorker", "❌ Exception during upload", e)
            Result.retry()
        }
    }
}
