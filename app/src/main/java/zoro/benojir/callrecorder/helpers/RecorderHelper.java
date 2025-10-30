package zoro.benojir.callrecorder.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.UUID;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;
import kotlinx.coroutines.BuildersKt;
import zoro.benojir.callrecorder.data.CallRecordEntity;
import zoro.benojir.callrecorder.data.CallRecordRepository;

public class RecorderHelper {

    private static final String TAG = "MADARA";
    private final Context context;
    public static MediaRecorder recorder;
    private final String phoneNumber;
    private final SharedPreferences preferences;

    // -------------------------------------------------------------------
    private long startTimeMillis;
    private String callId;
    private String callType = "unknown";   // inbound / outbound / internal
    private String callStatus = "answered"; // answered / no_answer / busy / failed
    // -------------------------------------------------------------------

    public RecorderHelper(Context context, String phoneNumber) {
        this.context = context;
        this.phoneNumber = phoneNumber;
        this.callId = UUID.randomUUID().toString();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setCallType(String type) {
        this.callType = type;
    }

    public void setCallStatus(String status) {
        this.callStatus = status;
    }

    // -------------------------------------------------------------------

    public void startRecoding() {
        startTimeMillis = System.currentTimeMillis();
        File directory = context.getExternalFilesDir("/recordings/");

        if (directory != null && !directory.exists()) {
            if (!directory.mkdirs()) {
                Toast.makeText(context, "Failed to create directory.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String fileName = directory.getAbsolutePath() + "/" + getFileName();

        recorder = new MediaRecorder();
        recorder.reset();
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setAudioEncodingBitRate(16);
        recorder.setAudioSamplingRate(44100);
        recorder.setOutputFile(fileName);

        try {
            recorder.prepare();
            recorder.start();

            if (preferences.getBoolean("start_toast", false)) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Recording started!", Toast.LENGTH_SHORT).show()
                );
            }

            Log.d(TAG, "🎙️ Recording started for " + phoneNumber + ", callId=" + callId);

        } catch (Exception e) {
            Log.e(TAG, "startVoiceRecoding: ", e);
            recorder = null;
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "Recording start failed!", Toast.LENGTH_SHORT).show()
            );
        }
    }

    // -------------------------------------------------------------------

    public void stopVoiceRecoding() {
        long endTimeMillis = System.currentTimeMillis();
        long durationSeconds = (endTimeMillis - startTimeMillis) / 1000;

        try {
            if (recorder != null) {
                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;
            }

            File directory = context.getExternalFilesDir("/recordings/");
            if (directory != null && directory.exists()) {
                File[] files = directory.listFiles();
                if (files != null && files.length > 0) {
                    File lastFile = files[files.length - 1];
                    Log.d(TAG, "Recording saved: " + lastFile.getAbsolutePath());

                    // ✅ Save call metadata to local DB
                    saveCallMetadata(
                            lastFile.getAbsolutePath(),
                            durationSeconds,
                            startTimeMillis,
                            endTimeMillis
                    );

                    // ✅ Enqueue upload work
                    UploadWorkHelper.INSTANCE.enqueueVoiceUpload(
                            context,
                            lastFile.getAbsolutePath()
                    );
                }
            }

            if (preferences.getBoolean("saved_toast", false)) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Recording saved successfully!", Toast.LENGTH_SHORT).show()
                );
            }

        } catch (Exception e) {
            Log.e(TAG, "stopVoiceRecoding:", e);
            recorder = null;

            if (preferences.getBoolean("saved_toast", false)) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Recording saved!", Toast.LENGTH_SHORT).show()
                );
            }
        }
    }

    // -------------------------------------------------------------------

    private void saveCallMetadata(String filePath, long duration, long startTime, long endTime) {
        // ⚙️ Run in background thread (Kotlin coroutine from Java)
        BuildersKt.launch(GlobalScope.INSTANCE, Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (scope, continuation) -> {
            try {
                CallRecordRepository repo = new CallRecordRepository(context);

                CallRecordEntity entity = new CallRecordEntity(
                        0,
                        callId,
                        phoneNumber,
                        callType,
                        callStatus,
                        duration,
                        startTime,
                        endTime,
                        filePath,
                        false
                );

                repo.insert(entity);
                Log.i(TAG, "📀 Call metadata saved: " + entity);

            } catch (Exception ex) {
                Log.e(TAG, "❌ Failed to save call metadata", ex);
            }

            return Unit.INSTANCE;
        });
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public String getCallType() { return callType; }
    public String getCallStatus() { return callStatus; }

    // -------------------------------------------------------------------

    private String getFileName() {
//        String contactName = ContactsHelper.getContactNameByPhoneNumber(context, phoneNumber);
//        if (contactName == null || contactName.isEmpty()) contactName = "Unknown";
        String username = CustomFunctions.getUserName(context.getApplicationContext());

        return callId+"_("
                + phoneNumber
                + ")_"
                + DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime())
                + ".m4a";
    }
}
