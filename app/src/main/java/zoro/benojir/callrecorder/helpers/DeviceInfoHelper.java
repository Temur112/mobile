package zoro.benojir.callrecorder.helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import java.util.List;

public class DeviceInfoHelper {

    public static String getOwnPhoneNumber(Context context) {
        try {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                            != PackageManager.PERMISSION_GRANTED) {
                Log.w("DeviceInfoHelper", "⚠️ Missing READ_PHONE_NUMBERS permission");
                return "unknown";
            }

            // Try simple method first
            String number = telephonyManager.getLine1Number();
            if (number != null && !number.isEmpty()) return number;

            // Try dual SIM support
            SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (sm != null) {
                List<SubscriptionInfo> subs = sm.getActiveSubscriptionInfoList();
                if (subs != null && !subs.isEmpty()) {
                    number = subs.get(0).getNumber();
                    if (number != null && !number.isEmpty()) return number;
                }
            }

            return "unknown";
        } catch (Exception e) {
            Log.e("DeviceInfoHelper", "❌ Failed to get device number", e);
            return "error";
        }
    }
}
