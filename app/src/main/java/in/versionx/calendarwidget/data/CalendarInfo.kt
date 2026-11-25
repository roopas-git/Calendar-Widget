package `in`.versionx.calendarwidget.data

/** One calendar available on the device (Google Calendar, another synced account, or local). */
data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int = CalendarEvent.DEFAULT_COLOR,
)
