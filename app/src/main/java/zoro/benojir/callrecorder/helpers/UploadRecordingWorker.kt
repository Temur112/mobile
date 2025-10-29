package zoro.benojir.callrecorder.helpers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import zoro.benojir.callrecorder.data.AppDatabase
import java.io.File
import java.util.concurrent.TimeUnit

class UploadRecordingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "UploadRecordingWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "🚀 Worker started!") // 🟢 1️⃣ Top-level start

        val filePath = inputData.getString("file_path") ?: ""
        Log.d(TAG, "📁 Received filePath='$filePath'") // 🟢 2️⃣ See what data was passed

        val hasFile = filePath.isNotEmpty() && File(filePath).exists()
        val file = if (hasFile) File(filePath) else null

        val token = CustomFunctions.getToken(applicationContext)
        var serverUrl = CustomFunctions.getServerUrl(applicationContext)
        Log.d(TAG, "🔑 Token exists=${!token.isNullOrEmpty()}, ServerUrl=$serverUrl") // 🟢 3️⃣ Confirm setup

        if (token.isNullOrEmpty() || serverUrl.isNullOrEmpty()) {
            Log.e(TAG, "❌ Missing token or server URL")
            return Result.failure()
        }

        if (!serverUrl.endsWith("/")) serverUrl += "/"
        val fullUrl = serverUrl + "voice"

        val db = AppDatabase.getInstance(applicationContext)
        val unsyncedRecords = db.callRecordDao().getUnsyncedRecords()

        Log.d(TAG, "📦 Found ${unsyncedRecords.size} unsynced records") // 🟢 4️⃣ Check if DB actually has items

        if (unsyncedRecords.isEmpty()) {
            Log.d(TAG, "📭 No unsynced call records.")
            return Result.success()
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        for (record in unsyncedRecords) {
            val recordHasFile = record.filePath.isNotEmpty() && File(record.filePath).exists()
            val recordFile = if (recordHasFile) File(record.filePath) else null

            Log.d(TAG, "🧾 Preparing record: id=${record.id}, call_id=${record.callId}, hasFile=$recordHasFile, status=${record.callStatus}")

            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

            if (recordHasFile && recordFile != null) {
                builder.addFormDataPart(
                    "file",
                    recordFile.name,
                    RequestBody.create("audio/m4a".toMediaTypeOrNull(), recordFile)
                )
            }

            builder
                .addFormDataPart("call_id", record.callId)
                .addFormDataPart("call_type", record.callType)
                .addFormDataPart("call_status", record.callStatus)
                .addFormDataPart("to", record.phoneNumber)
                .addFormDataPart("duration", record.duration.toString())
                .addFormDataPart("start_time", record.startTime.toString())
                .addFormDataPart("end_time", record.endTime.toString())

            val requestBody = builder.build()

            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            try {
                Log.d(TAG, "📤 Uploading: ${record.callStatus} | file=${record.filePath}")
                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""

                Log.d(TAG, "📩 Response ${response.code}: $responseText")

                if (response.isSuccessful) {
                    db.callRecordDao().markAsSynced(record.id)
                    Log.i(
                        TAG,
                        "✅ Uploaded & synced record: ${record.callId} (${record.callStatus})"
                    )
                } else {
                    Log.w(TAG, "⚠️ Upload failed (code ${response.code}) for ${record.callId}")
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
