package `in`.versionx.calendarwidget.notification

import android.content.Context
import `in`.versionx.calendarwidget.data.CalendarEvent

/**
 * Tracks which event occurrences have already been notified about, so the daily
 * background check doesn't re-notify the same event every day while it stays
 * inside the 7-day lookahead window. Backed by SharedPreferences since the set
 * of pending keys is small and short-lived.
 */
class NotifiedEventStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasBeenNotified(event: CalendarEvent): Boolean =
        prefs.contains(keyFor(event))

    fun markNotified(event: CalendarEvent) {
        prefs.edit().putLong(keyFor(event), event.instanceStartMillis).apply()
    }

    /** Drops stored keys for occurrences whose start time has already passed, to stop growing forever. */
    fun pruneOlderThan(cutoffMillis: Long) {
        val stale = prefs.all.filterValues { (it as? Long ?: 0L) < cutoffMillis }.keys
        if (stale.isEmpty()) return
        val editor = prefs.edit()
        stale.forEach { editor.remove(it) }
        editor.apply()
    }

    private fun keyFor(event: CalendarEvent) = event.instanceKey

    private companion object {
        const val PREFS_NAME = "notified_events"
    }
}
