package zoro.benojir.callrecorder.helpers

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import zoro.benojir.callrecorder.data.AppDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class UploadRecordingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "UploadRecordingWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "🚀 Worker started!")

        val token = CustomFunctions.getToken(applicationContext)
        var serverUrl = CustomFunctions.getServerUrl(applicationContext)
        val username = CustomFunctions.getUserName(applicationContext)  // or dynamically loaded from preferences

        if (token.isNullOrEmpty() || serverUrl.isNullOrEmpty()) {
            Log.e(TAG, "❌ Missing token or server URL")
            return Result.failure()
        }

        if (!serverUrl.endsWith("/")) serverUrl += "/"
        val fullUrl = serverUrl + "api/call/v1/upload"

        val db = AppDatabase.getInstance(applicationContext)
        val unsyncedRecords = db.callRecordDao().getUnsyncedRecords()
        Log.d(TAG, "📦 Found ${unsyncedRecords.size} unsynced records")

        if (unsyncedRecords.isEmpty()) {
            Log.d(TAG, "📭 No unsynced call records.")
            return Result.success()
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        for (record in unsyncedRecords) {
            try {
                Log.d(TAG, "🧾 Preparing record: id=${record.id}, call_id=${record.callId}, status=${record.callStatus}")

                val file = File(record.filePath)
                val hasFile = file.exists() && file.isFile

                var audioBase64: String? = null
                if (hasFile) {
                    val bytes = file.readBytes()
                    audioBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }


                var receiver = ""
                var sender = ""

                if (record.callType.lowercase() == "inbound"){
                    receiver = record.phoneNumber
                    sender = record.phoneNumber
                }else{
                    receiver = record.phoneNumber
                    sender = record.phoneNumber
                }

                val formattedStartTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(record.startTime))

                val formattedEndTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(record.endTime))

                val correctStatus = if (record.callStatus.toString().lowercase() == "rejected") {
                    "no_answer"
                } else {
                    record.callStatus.toString().lowercase()
                }

                val json = JSONObject().apply {
                    put("user_name", username)
                    put("api_key", token)
                    put("call_id", record.id)
                    put("call_type", record.callType)
                    put("call_status", correctStatus)
                    put("from", sender)
                    put("to", receiver)
                    put("duration", record.duration)
                    put("start_time", formattedStartTime)
                    put("end_time", formattedEndTime)
                    put("audio_filename", if (hasFile) file.name else "none")
                    put("audio_file", audioBase64 ?: JSONObject.NULL)
                }

                val body = json.toString().toRequestBody("application/json".toMediaType())
                Log.d("DATA", "the voice upload body $json")
                val request = Request.Builder()
                    .url(fullUrl)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                Log.d(TAG, "📤 Uploading call ${record.callId}")
                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""

                Log.d(TAG, "📩 Response ${response.code}: $responseText")

                if (response.isSuccessful) {
                    db.callRecordDao().markAsSynced(record.id)
                    Log.i(TAG, "✅ Uploaded & synced record: ${record.callId}")
                } else {
                    Log.w(TAG, "⚠️ Upload failed (${response.code}) for ${record.callId}")
                    return Result.retry()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Upload error for record ${record.callId}", e)
                return Result.retry()
            }
        }

        Log.d(TAG, "🏁 Worker finished successfully")
        return Result.success()
    }
}
