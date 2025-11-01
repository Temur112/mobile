package zoro.benojir.callrecorder.helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class DeviceInfoHelper {

    public static List<String> getOwnPhoneNumbers(Context context) {
        List<String> numbers = new ArrayList<>();

        try {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                            != PackageManager.PERMISSION_GRANTED) {

                Log.w("DeviceInfoHelper", "⚠️ Missing READ_PHONE_NUMBERS permission");
                numbers.add("Permission not granted");
                return numbers;
            }

            // ✅ Try basic method first
            String mainNumber = telephonyManager.getLine1Number();
            if (mainNumber != null && !mainNumber.isEmpty()) {
                numbers.add(mainNumber);
            }

            // ✅ Try to fetch all SIMs (dual SIM, etc.)
            SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (sm != null) {
                List<SubscriptionInfo> subs = sm.getActiveSubscriptionInfoList();
                if (subs != null && !subs.isEmpty()) {
                    for (SubscriptionInfo sub : subs) {
                        String number = sub.getNumber();
                        String carrier = sub.getCarrierName() != null ? sub.getCarrierName().toString() : "Unknown carrier";
                        int simSlot = sub.getSimSlotIndex();

                        if (number == null || number.isEmpty()) {
                            number = "(no number stored on SIM)";
                        }

                        numbers.add("SIM " + (simSlot + 1) + " (" + carrier + "): " + number);
                    }
                }
            }

            if (numbers.isEmpty()) {
                numbers.add("No SIM numbers available");
            }

            return numbers;

        } catch (Exception e) {
            Log.e("DeviceInfoHelper", "❌ Failed to get device number", e);
            numbers.add("Error: " + e.getMessage());
            return numbers;
        }
    }
}
