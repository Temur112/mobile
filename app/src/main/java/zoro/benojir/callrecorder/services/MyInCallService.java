package zoro.benojir.callrecorder.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;
import zoro.benojir.callrecorder.BuildConfig;
import zoro.benojir.callrecorder.R;
import zoro.benojir.callrecorder.data.CallRecordEntity;
import zoro.benojir.callrecorder.data.CallRecordRepository;
import zoro.benojir.callrecorder.helpers.RecorderHelper;

public class MyInCallService extends InCallService {

    public static final String CHANNEL_ID = BuildConfig.APPLICATION_ID;
    private final Map<Call, RecorderHelper> activeRecorders = new HashMap<>();
    private final Map<Call, Long> callStartTimes = new HashMap<>();
    private Context sContext;

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        sContext = this;

        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                super.onStateChanged(call, state);
                Log.d("MyInCallService", "📞 Call state changed: " + state);

                switch (state) {
                    case Call.STATE_RINGING:
                        Log.d("MyInCallService", "Incoming call ringing");
                        handleCallStart(call, "inbound", "ringing");
                        break;

                    case Call.STATE_DIALING:
                        Log.d("MyInCallService", "Outgoing call dialing");
                        handleCallStart(call, "outbound", "dialing");
                        break;

                    case Call.STATE_ACTIVE:
                        Log.d("MyInCallService", "Call answered (active)");
                        startRecording(call);
                        break;

                    case Call.STATE_DISCONNECTED:
                    case Call.STATE_DISCONNECTING:
                        Log.d("MyInCallService", "Call ended");

                        // 🧠 Determine whether call was ever active (answered)
                        boolean wasAnswered = (RecorderHelper.recorder != null);
                        RecorderHelper helper = activeRecorders.remove(call);

                        if (helper == null) {
                            // ✅ No helper created — create one to save metadata
                            String number = call.getDetails().getHandle().getSchemeSpecificPart();
                            helper = new RecorderHelper(sContext, number);
                            boolean isIncoming = (call.getDetails().getCallDirection() == Call.Details.DIRECTION_INCOMING);
                            helper.setCallType(isIncoming ? "inbound" : "outbound");
                        }

                        // 🧭 Determine status from cause
                        int cause = call.getDetails().getDisconnectCause().getCode();
                        String status = mapDisconnectCauseToStatus(cause);
                        helper.setCallStatus(status);

                        if (wasAnswered) {
                            Log.d("MyInCallService", "🎙️ Answered call ended → stopping recording");
                            helper.stopVoiceRecoding();
                        } else {
                            Log.d("MyInCallService", "🟡 Missed/rejected/unanswered call → saving metadata only");
                            saveMetadataOnly(helper, 0, System.currentTimeMillis(), System.currentTimeMillis());
                        }

                        if (activeRecorders.isEmpty()) {
                            stopForeground(true);
                            Log.d("MyInCallService", "🟢 Foreground service stopped (no active recorders).");
                        }
                        break;
                }
            }
        });
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        handleCallEnd(call);
    }

    // ----------------------------------------------------------
    // 🟢 Start recording for answered calls
    private void startRecording(Call call) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(sContext);
        if (preferences.getBoolean("is_call_recording_enabled", false)) {
            createNotificationChannel();

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(sContext, CHANNEL_ID)
                    .setContentTitle("Call Recorder")
                    .setContentText("Call recording in progress")
                    .setSmallIcon(R.drawable.keyboard_voice)
                    .setOngoing(true);

            Notification notification = notificationBuilder.build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(1, notification);
            }

            RecorderHelper recorderHelper = new RecorderHelper(
                    sContext,
                    call.getDetails().getHandle().getSchemeSpecificPart()
            );

            boolean isIncoming = (call.getDetails().getCallDirection() == Call.Details.DIRECTION_INCOMING);
            recorderHelper.setCallType(isIncoming ? "inbound" : "outbound");
            recorderHelper.setCallStatus("answered");

            recorderHelper.startRecoding();
            activeRecorders.put(call, recorderHelper);
        }
    }

    // ----------------------------------------------------------
    // 🟡 Handle when a call starts (ringing or dialing)
    private void handleCallStart(Call call, String type, String status) {
        callStartTimes.put(call, System.currentTimeMillis());

        RecorderHelper recorderHelper = new RecorderHelper(
                sContext,
                call.getDetails().getHandle().getSchemeSpecificPart()
        );
        recorderHelper.setCallType(type);
        recorderHelper.setCallStatus(status);

        activeRecorders.put(call, recorderHelper);
    }

    // ----------------------------------------------------------
    // 🔴 Handle when a call ends (including missed, rejected, failed)
    private void handleCallEnd(Call call) {
        long endTime = System.currentTimeMillis();
        long startTime = callStartTimes.getOrDefault(call, endTime);
        long duration = (endTime - startTime) / 1000;

        Log.d("MyInCallService", "🟣 handleCallEnd() called. Duration=" + duration + "s");

        RecorderHelper recorderHelper = activeRecorders.remove(call);

        if (recorderHelper == null) {
            Log.w("MyInCallService", "⚠️ No RecorderHelper found for this call!");
            return;
        }

        int cause = call.getDetails().getDisconnectCause().getCode();
        String status = mapDisconnectCauseToStatus(cause);
        recorderHelper.setCallStatus(status);

        Log.d("MyInCallService", "🧭 Disconnect cause=" + cause + ", mapped status=" + status);

        if (RecorderHelper.recorder != null) {
            Log.d("MyInCallService", "🎙️ Recorder still active → stopping recording...");
            recorderHelper.stopVoiceRecoding();
        } else {
            Log.d("MyInCallService", "🟡 No active recorder → saving metadata only.");
            saveMetadataOnly(recorderHelper, duration, startTime, endTime);
        }

        if (activeRecorders.isEmpty()) {
            stopForeground(true);
            Log.d("MyInCallService", "🟢 Foreground service stopped (no active recorders).");
        }
    }


    // ----------------------------------------------------------
    // 🧠 Translate system disconnect codes to meaningful statuses
    private String mapDisconnectCauseToStatus(int code) {
        switch (code) {
            case android.telecom.DisconnectCause.MISSED:
                return "no_answer";
            case android.telecom.DisconnectCause.REJECTED:
                return "rejected";
            case android.telecom.DisconnectCause.BUSY:
                return "busy";
            case android.telecom.DisconnectCause.ERROR:
                return "failed";
            default:
                return "answered";
        }
    }

    // ----------------------------------------------------------
    // 💾 Save metadata when no recording exists
    private void saveMetadataOnly(RecorderHelper helper, long duration, long start, long end) {
        Log.d("MyInCallService", "🟡 saveMetadataOnly() triggered for " + helper.getPhoneNumber());

        BuildersKt.launch(
                GlobalScope.INSTANCE,
                Dispatchers.getIO(),
                kotlinx.coroutines.CoroutineStart.DEFAULT,
                (scope, continuation) -> {
                    try {
                        CallRecordRepository repo = new CallRecordRepository(sContext);
                        String fixedStatus = helper.getCallType();
                        Log.d("METADATA", "🔁 Adjusted status");
                        if ("outbound".equalsIgnoreCase(helper.getCallType())  && !"busy".equalsIgnoreCase(helper.getCallStatus()) && duration == 0L) {
                            fixedStatus = "no_answer";
                            Log.d("METADATA", "🔁 Adjusted status to 'no_answer' for outbound call with 0 duration." + helper.getCallStatus() + helper.getCallType());
                            helper.setCallStatus(fixedStatus);
                        }


                        CallRecordEntity entity = new CallRecordEntity(
                                0,
                                UUID.randomUUID().toString(),
                                helper.getPhoneNumber(),
                                helper.getCallType(),
                                helper.getCallStatus(),
                                duration,
                                start,
                                end,
                                "",  // ❌ no file
                                false
                        );

                        // save to DB
                        repo.insertBlocking(entity);

                        // ✅ enqueue upload worker for metadata
                        zoro.benojir.callrecorder.helpers.UploadWorkHelper.INSTANCE.enqueueVoiceUpload(
                                sContext,
                                "" // empty path; worker will detect metadata-only
                        );
                        Log.d("MyInCallService", "📦 Enqueued upload worker for metadata-only record");


                        Log.i("MyInCallService", "📄 Metadata-only call saved and enqueued: " + entity);

                    } catch (Exception e) {
                        Log.e("MyInCallService", "❌ Failed to save metadata-only call", e);
                    }
                    return kotlin.Unit.INSTANCE;
                }
        );
    }

    // ----------------------------------------------------------
    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Call Recorder Service Channel",
                NotificationManager.IMPORTANCE_NONE
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }
}
