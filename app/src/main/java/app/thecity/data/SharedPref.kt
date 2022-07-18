package app.thecity.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import app.thecity.data.SharedPref
import android.text.TextUtils
import android.preference.PreferenceManager
import app.thecity.R

class SharedPref(private val context: Context) {
    private val sharedPreferences: SharedPreferences
    private val prefs: SharedPreferences
    var fcmRegId: String?
        get() = sharedPreferences.getString(FCM_PREF_KEY, null)
        set(gcmRegId) {
            sharedPreferences.edit().putString(FCM_PREF_KEY, gcmRegId).apply()
        }
    val isFcmRegIdEmpty: Boolean
        get() = TextUtils.isEmpty(fcmRegId)
    var isRegisteredOnServer: Boolean
        get() = sharedPreferences.getBoolean(SERVER_FLAG_KEY, false)
        set(registered) {
            sharedPreferences.edit().putBoolean(SERVER_FLAG_KEY, registered).apply()
        }
    val isNeedRegisterFcm: Boolean
        get() = isFcmRegIdEmpty || !isRegisteredOnServer

    /**
     * For notifications flag
     */
    val notification: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_key_notif), true)
    val ringtone: String?
        get() = prefs.getString(
            context.getString(R.string.pref_key_ringtone),
            "content://settings/system/notification_sound"
        )
    val vibration: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_key_vibrate), true)

    /**
     * Refresh user data
     * When phone receive GCM notification this flag will be enable.
     * so when user open the app all data will be refresh
     */
    var isRefreshPlaces: Boolean
        get() = sharedPreferences.getBoolean(REFRESH_PLACES, false)
        set(need_refresh) {
            sharedPreferences.edit().putBoolean(REFRESH_PLACES, need_refresh).apply()
        }

    /**
     * For theme color
     */
    var themeColor: String?
        get() = sharedPreferences.getString(THEME_COLOR_KEY, "")
        set(color) {
            sharedPreferences.edit().putString(THEME_COLOR_KEY, color).apply()
        }
    val themeColorInt: Int
        get() = if (themeColor == "") {
            context.resources.getColor(R.color.colorPrimary)
        } else Color.parseColor(themeColor)

    /**
     * To save last state request
     */
    var lastPlacePage: Int
        get() = sharedPreferences.getInt(LAST_PLACE_PAGE, 1)
        set(page) {
            sharedPreferences.edit().putInt(LAST_PLACE_PAGE, page).apply()
        }

    /**
     * To save dialog permission state
     */
    fun setNeverAskAgain(key: String?, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun getNeverAskAgain(key: String?): Boolean {
        return sharedPreferences.getBoolean(key, false)
    }

    // Preference for first launch
    var intersCounter: Int
        get() = sharedPreferences.getInt("INTERS_COUNT", 0)
        set(counter) {
            sharedPreferences.edit().putInt("INTERS_COUNT", counter).apply()
        }

    fun clearIntersCounter() {
        sharedPreferences.edit().putInt("INTERS_COUNT", 0).apply()
    }

    companion object {
        const val MAX_OPEN_COUNTER = 0
        private const val FCM_PREF_KEY = "app.thecity.data.FCM_PREF_KEY"
        private const val SERVER_FLAG_KEY = "app.thecity.data.SERVER_FLAG_KEY"
        private const val THEME_COLOR_KEY = "app.thecity.data.THEME_COLOR_KEY"
        private const val LAST_PLACE_PAGE = "LAST_PLACE_PAGE_KEY"

        // need refresh
        const val REFRESH_PLACES = "app.thecity.data.REFRESH_PLACES"
    }

    init {
        sharedPreferences = context.getSharedPreferences("MAIN_PREF", Context.MODE_PRIVATE)
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }
}