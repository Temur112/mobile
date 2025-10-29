package zoro.benojir.callrecorder.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object PinPreferencesHelper {
    private const val KEY_PIN = "user_pin"
    private const val KEY_PIN_ENABLED = "enable_pin"
    private const val KEY_SESSION_UNLOCKED = "pin_session_unlocked"
    private const val KEY_LAST_UNLOCK_TIME = "pin_last_unlock_time"

    private fun prefs(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    @JvmStatic
    fun isPinEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PIN_ENABLED, false)

    @JvmStatic
    fun getPin(context: Context): String? =
        prefs(context).getString(KEY_PIN, null)

    @JvmStatic
    fun setPin(context: Context, pin: String) {
        prefs(context).edit().putString(KEY_PIN, pin).apply()
    }

    // ✅ Session and timeout helpers
    @JvmStatic
    fun setSessionUnlocked(context: Context, unlocked: Boolean) {
        prefs(context).edit().putBoolean(KEY_SESSION_UNLOCKED, unlocked).apply()
    }

    @JvmStatic
    fun isSessionUnlocked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SESSION_UNLOCKED, false)

    @JvmStatic
    fun updateLastUnlockTime(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_UNLOCK_TIME, System.currentTimeMillis()).apply()
    }

    @JvmStatic
    fun shouldRelock(context: Context, timeoutMinutes: Int = 5): Boolean {
        val lastTime = prefs(context).getLong(KEY_LAST_UNLOCK_TIME, 0L)
        if (lastTime == 0L) return true
        val diff = System.currentTimeMillis() - lastTime
        return diff > timeoutMinutes * 60_000
    }
}
