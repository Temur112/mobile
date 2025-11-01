package zoro.benojir.callrecorder.helpers

import android.R
import android.app.DirectAction
import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import zoro.benojir.callrecorder.data.AppDatabase
import java.util.concurrent.TimeUnit
import zoro.benojir.callrecorder.helpers.CustomFunctions

class SmsUploadHelper {
    companion object {
        private var lastUploadedBody: String? = null
        private var lastUploadedReceiver: String? = null
        private var lastUploadTime: Long = 0

        fun enqueueSmsUpload(context: Context, sender: String, receiver: String, text: String, action: String, username: String, smsid: String, status: String, direction: String) {
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
                "username" to username,
                "action" to action,
                "sms_id" to smsid,
                "sender" to sender,
                "receiver" to receiver,
                "text" to text,
                "status" to status,
                "direction" to direction,
                "timestamp" to System.currentTimeMillis()
            )


            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            val work = androidx.work.OneTimeWorkRequestBuilder<SmsUploadWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.LINEAR,
                    15,
                    TimeUnit.SECONDS
                )
                .build()
            Log.d("WORKER", "starting sms que worker")
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
        val smsId = inputData.getString("sms_id") ?: "0"
        val action = inputData.getString("action") ?: "receive"
        val username = inputData.getString("username") ?: ""
        val direction = inputData.getString("direction") ?: "unknown"
        val status = inputData.getString("status") ?: ""

        Log.d("SmsUploadWorker", "⚙️ Worker started execution")
        Log.d("SmsUploadWorker", "📦 Input data -> sender=$sender | receiver=$receiver | body=$body")

        try {
//            val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
//            val token = prefs.getString("auth_token", "") ?: ""
            val token = CustomFunctions.getToken(appContext)
            var serverUrl = CustomFunctions.getServerUrl(appContext)

            Log.d("SmsUploadWorker", "🌐 Server URL: $serverUrl")

            Log.d("SmsUploadWorker", "🔑 Loaded token: ${if (token.isNotEmpty()) "present (${token.take(10)}...)" else "missing"}")

            if (token.isEmpty()) {
                Log.e("SmsUploadWorker", "❌ No token found. Cannot upload SMS.")
                return@withContext Result.failure()
            }

            if (serverUrl.isEmpty()) {
                Log.e("SmsUploadWorker", "❌ No server URL found. Cannot upload SMS.")
            }

            if (!serverUrl.endsWith("/")) serverUrl += "/"

            val fullUrl = serverUrl + "api/call/v1/sms"
            Log.d("SmsUploadWorker", "🌐 Full URL: $fullUrl")



            // ✅ Build JSON payload
            val json = JSONObject().apply {
                put("api_key", token)
                put("sms_id", smsId)
                put("from_number", sender)
                put("to", receiver)
                put("message_text", body)
                put("user_name", username)
                put("status", status)
                put("direction", direction)
                put("created_time", inputData.getLong("timestamp", System.currentTimeMillis()).toString())
            }

            val jsonStr = json.toString()
            Log.d("SmsUploadWorker", "🧾 JSON body -> $jsonStr")

            val requestBody = jsonStr.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(fullUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d("SmsUploadWorker", "🌐 Sending POST request to " + request.url)

            val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                Log.d("SmsUploadWorker", "📩 Response code=${response.code}, body=$responseBody")

                return@withContext when {
                    response.isSuccessful -> {
                        Log.i("SmsUploadWorker", "✅ SMS uploaded successfully")
                        val dao = AppDatabase.getInstance(appContext).smsDao()
                        dao.updateSyncedStatus(receiver, body, true)
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
