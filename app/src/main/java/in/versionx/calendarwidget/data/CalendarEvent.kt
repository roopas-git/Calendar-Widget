package `in`.versionx.calendarwidget.data

/**
 * One occurrence of a calendar event (an instance, not the recurring series),
 * as read from the device's CalendarContract provider.
 */
data class CalendarEvent(
    val eventId: Long,
    val instanceStartMillis: Long,
    val instanceEndMillis: Long,
    val title: String,
    val calendarDisplayName: String,
    val allDay: Boolean,
    /** The event's display color (its own color, else its calendar's), as ARGB. */
    val color: Int = DEFAULT_COLOR,
) {
    /** Stable key for an occurrence, used to de-duplicate notifications. */
    val instanceKey: String get() = "$eventId:$instanceStartMillis"

    companion object {
        const val DEFAULT_COLOR: Int = 0xFF4285F4.toInt()
    }
}
