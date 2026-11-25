package `in`.versionx.calendarwidget.widget

import android.content.Context

/** Stores [WidgetAppearance] per `appWidgetId`, since each widget instance is configured separately. */
object WidgetAppearancePrefs {

    private const val PREFS_NAME = "widget_appearance"

    private val INT_FIELDS = listOf("background", "text", "today", "opacity", "radius", "textSize", "date", "gradientEnd", "tomorrow")
    private val BOOL_FIELDS = listOf("gradient", "calendarColors", "dateBlock")

    fun get(context: Context, appWidgetId: Int): WidgetAppearance {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val d = WidgetAppearance.DEFAULT
        return WidgetAppearance(
            backgroundColor = prefs.getInt(key(appWidgetId, "background"), d.backgroundColor),
            textColor = prefs.getInt(key(appWidgetId, "text"), d.textColor),
            todayColor = prefs.getInt(key(appWidgetId, "today"), d.todayColor),
            backgroundOpacityPercent = prefs.getInt(key(appWidgetId, "opacity"), d.backgroundOpacityPercent),
            cornerRadiusDp = prefs.getInt(key(appWidgetId, "radius"), d.cornerRadiusDp),
            textSizeSp = prefs.getInt(key(appWidgetId, "textSize"), d.textSizeSp),
            dateColor = prefs.getInt(key(appWidgetId, "date"), d.dateColor),
            gradientEnabled = prefs.getBoolean(key(appWidgetId, "gradient"), d.gradientEnabled),
            gradientEndColor = prefs.getInt(key(appWidgetId, "gradientEnd"), d.gradientEndColor),
            showCalendarColors = prefs.getBoolean(key(appWidgetId, "calendarColors"), d.showCalendarColors),
            showDateBlock = prefs.getBoolean(key(appWidgetId, "dateBlock"), d.showDateBlock),
            tomorrowColor = prefs.getInt(key(appWidgetId, "tomorrow"), d.tomorrowColor),
        )
    }

    fun set(context: Context, appWidgetId: Int, appearance: WidgetAppearance) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(key(appWidgetId, "background"), appearance.backgroundColor)
            .putInt(key(appWidgetId, "text"), appearance.textColor)
            .putInt(key(appWidgetId, "today"), appearance.todayColor)
            .putInt(key(appWidgetId, "opacity"), appearance.backgroundOpacityPercent)
            .putInt(key(appWidgetId, "radius"), appearance.cornerRadiusDp)
            .putInt(key(appWidgetId, "textSize"), appearance.textSizeSp)
            .putInt(key(appWidgetId, "date"), appearance.dateColor)
            .putBoolean(key(appWidgetId, "gradient"), appearance.gradientEnabled)
            .putInt(key(appWidgetId, "gradientEnd"), appearance.gradientEndColor)
            .putBoolean(key(appWidgetId, "calendarColors"), appearance.showCalendarColors)
            .putBoolean(key(appWidgetId, "dateBlock"), appearance.showDateBlock)
            .putInt(key(appWidgetId, "tomorrow"), appearance.tomorrowColor)
            .apply()
    }

    /** Called when a widget instance is removed, so its settings don't linger forever. */
    fun delete(context: Context, appWidgetId: Int) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        (INT_FIELDS + BOOL_FIELDS).forEach { editor.remove(key(appWidgetId, it)) }
        editor.apply()
    }

    private fun key(appWidgetId: Int, field: String) = "${appWidgetId}_$field"
}
