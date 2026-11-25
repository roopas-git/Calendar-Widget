package `in`.versionx.calendarwidget.notification

import `in`.versionx.calendarwidget.data.CalendarEvent
import `in`.versionx.calendarwidget.widget.EventLineFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * When to remind about an event:
 * - timed events: [MINUTES_BEFORE] minutes before they start;
 * - all-day events (birthdays, holidays): at [ALL_DAY_HOUR]:00 on that day.
 */
object ReminderTimes {

    const val MINUTES_BEFORE = 15
    const val ALL_DAY_HOUR = 9

    /** How far ahead each background check schedules reminders. Checks run at least daily. */
    val SCHEDULE_HORIZON_MILLIS: Long = TimeUnit.HOURS.toMillis(48)

    fun notifyAt(event: CalendarEvent): Long =
        if (event.allDay) {
            Calendar.getInstance().apply {
                timeInMillis = EventLineFormatter.eventDayStart(event)
                set(Calendar.HOUR_OF_DAY, ALL_DAY_HOUR)
            }.timeInMillis
        } else {
            event.instanceStartMillis - TimeUnit.MINUTES.toMillis(MINUTES_BEFORE.toLong())
        }

    /**
     * After this moment a reminder is pointless: a timed event has started, or an
     * all-day event's day is over. Before it, a late reminder (e.g. the phone was off
     * at [notifyAt]) is still shown.
     */
    fun remindUntil(event: CalendarEvent): Long =
        if (event.allDay) {
            Calendar.getInstance().apply {
                timeInMillis = EventLineFormatter.eventDayStart(event)
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
        } else {
            event.instanceStartMillis
        }

    /** The time to fire a reminder for [event], or null if it shouldn't be scheduled now. */
    fun scheduleTime(event: CalendarEvent, now: Long): Long? {
        if (now >= remindUntil(event)) return null
        val at = notifyAt(event)
        if (at > now + SCHEDULE_HORIZON_MILLIS) return null
        return maxOf(at, now)
    }
}
