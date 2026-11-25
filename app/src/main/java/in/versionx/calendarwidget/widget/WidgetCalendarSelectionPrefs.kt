package `in`.versionx.calendarwidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * Stores which calendars feed one widget instance (the old app's "Calendar
 * Accounts" setting, see docs/05-OLD-APP-REFERENCE.md) — per `appWidgetId`, same
 * as [WidgetAppearance].
 *
 * A `null` result means "no explicit selection" — treated as *every* calendar on
 * the device, matching the default (unfiltered) behavior and keeping newly-added
 * calendar accounts visible automatically. Saving an explicit selection that
 * happens to include every currently-known calendar is deliberately *not* the same
 * as `null` — see [WidgetConfigureActivity] for where that distinction is made.
 */
object WidgetCalendarSelectionPrefs {

    private const val PREFS_NAME = "widget_calendar_selection"
    private const val KEY_PREFIX = "calendar_ids_"

    fun get(context: Context, appWidgetId: Int): Set<Long>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(key(appWidgetId), null) ?: return null
        return stored.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun set(context: Context, appWidgetId: Int, calendarIds: Set<Long>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putStringSet(key(appWidgetId), calendarIds.map { it.toString() }.toSet())
            .apply()
    }

    /** Clears any explicit selection, reverting this widget to "every calendar." */
    fun clear(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(key(appWidgetId))
            .apply()
    }

    /**
     * The calendars the rest of the app (main event list, reminders) should use: the
     * union of what every placed widget shows. Returns null ("every calendar") when no
     * widget is placed yet or any widget shows every calendar; an empty set only if
     * every widget has all its calendars unticked.
     */
    fun appCalendarFilter(context: Context): Set<Long>? {
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, SimpleCalendarWidgetProvider::class.java))
        if (ids.isEmpty()) return null
        val union = mutableSetOf<Long>()
        for (id in ids) {
            union += get(context, id) ?: return null
        }
        return union
    }

    private fun key(appWidgetId: Int) = "$KEY_PREFIX$appWidgetId"
}
