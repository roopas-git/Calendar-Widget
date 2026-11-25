package `in`.versionx.calendarwidget.data

import android.content.Context

/** How far ahead to look for events: a fixed number of days, or a fixed number of events. */
enum class LookaheadMode {
    DAYS,
    COUNT,
}

data class LookaheadSettings(val mode: LookaheadMode, val value: Int) {
    companion object {
        const val MIN_VALUE = 1
        const val MAX_VALUE = 60
        val DEFAULT = LookaheadSettings(LookaheadMode.DAYS, 7)
    }
}

/**
 * Persists the user's choice of lookahead mode (the old app's "number of upcoming
 * events" customization, generalized to also support the original day-based
 * lookahead). Shared by the in-app event list, the daily worker, and the widget so
 * all three stay in sync on whichever mode the user picked.
 */
object LookaheadPreferences {

    private const val PREFS_NAME = "lookahead_settings"
    private const val KEY_MODE = "mode"
    private const val KEY_VALUE = "value"

    fun get(context: Context): LookaheadSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_MODE, null)
            ?.let { runCatching { LookaheadMode.valueOf(it) }.getOrNull() }
            ?: LookaheadSettings.DEFAULT.mode
        val value = prefs.getInt(KEY_VALUE, LookaheadSettings.DEFAULT.value)
        return LookaheadSettings(mode, value)
    }

    fun set(context: Context, settings: LookaheadSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_MODE, settings.mode.name)
            .putInt(KEY_VALUE, settings.value)
            .apply()
    }
}
