package `in`.versionx.calendarwidget.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.util.concurrent.TimeUnit

/**
 * Reads upcoming events from every calendar synced on the device (Google Calendar,
 * other accounts, local calendars) via the shared CalendarContract.Instances table.
 * Requires android.permission.READ_CALENDAR to already be granted.
 */
class CalendarRepository(private val context: Context) {

    /** Every calendar available on the device, for a "which calendars feed this?" picker. */
    fun getAvailableCalendars(): List<CalendarInfo> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        val calendars = mutableListOf<CalendarInfo>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val colorCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
            while (cursor.moveToNext()) {
                calendars += CalendarInfo(
                    id = cursor.getLong(idCol),
                    displayName = cursor.getString(nameCol) ?: "(Unnamed calendar)",
                    accountName = cursor.getString(accountCol) ?: "",
                    color = opaqueOrDefault(cursor, colorCol),
                )
            }
        }
        return calendars
    }

    /**
     * Returns events per [settings] — either a fixed day range, or a fixed event count.
     * [calendarIds] restricts results to those calendars: null means every calendar on
     * the device (the default), an empty set means none (so no events).
     */
    fun getUpcomingEvents(
        settings: LookaheadSettings,
        fromMillis: Long = System.currentTimeMillis(),
        calendarIds: Set<Long>? = null,
    ): List<CalendarEvent> = when (settings.mode) {
        LookaheadMode.DAYS -> getUpcomingEventsByDays(settings.value, fromMillis, calendarIds)
        LookaheadMode.COUNT -> getUpcomingEventsByCount(settings.value, fromMillis, calendarIds = calendarIds)
    }

    /** Events starting from [fromMillis] up to [daysAhead] days later, soonest first. */
    fun getUpcomingEventsByDays(
        daysAhead: Int,
        fromMillis: Long = System.currentTimeMillis(),
        calendarIds: Set<Long>? = null,
    ): List<CalendarEvent> {
        val toMillis = fromMillis + TimeUnit.DAYS.toMillis(daysAhead.toLong())
        return queryInstances(fromMillis, toMillis, calendarIds)
    }

    /**
     * The next [count] events starting from [fromMillis], soonest first, searched
     * within a widening window (capped at [maxSearchDays]) since Instances requires
     * an explicit end bound.
     */
    fun getUpcomingEventsByCount(
        count: Int,
        fromMillis: Long = System.currentTimeMillis(),
        maxSearchDays: Int = 365,
        calendarIds: Set<Long>? = null,
    ): List<CalendarEvent> {
        var searchDays = INITIAL_COUNT_SEARCH_WINDOW_DAYS
        var events: List<CalendarEvent> = emptyList()
        while (events.size < count && searchDays <= maxSearchDays) {
            val toMillis = fromMillis + TimeUnit.DAYS.toMillis(searchDays.toLong())
            events = queryInstances(fromMillis, toMillis, calendarIds)
            if (events.size >= count || searchDays == maxSearchDays) break
            searchDays = (searchDays * 2).coerceAtMost(maxSearchDays)
        }
        return events.take(count)
    }

    /**
     * True if the occurrence of [eventId] starting at [beginMillis] still exists — used
     * before showing a scheduled reminder, in case the event was deleted or moved.
     */
    fun instanceExists(eventId: Long, beginMillis: Long): Boolean {
        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, beginMillis - 1)
        ContentUris.appendId(uriBuilder, beginMillis + 1)
        context.contentResolver.query(
            uriBuilder.build(),
            arrayOf(CalendarContract.Instances.EVENT_ID),
            "${CalendarContract.Instances.EVENT_ID} = ? AND ${CalendarContract.Instances.BEGIN} = ?",
            arrayOf(eventId.toString(), beginMillis.toString()),
            null,
        )?.use { return it.moveToFirst() }
        return false
    }

    private fun queryInstances(fromMillis: Long, toMillis: Long, calendarIds: Set<Long>?): List<CalendarEvent> {
        if (calendarIds != null && calendarIds.isEmpty()) return emptyList()
        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, fromMillis)
        ContentUris.appendId(uriBuilder, toMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
        )

        val selection: String?
        val selectionArgs: Array<String>?
        if (calendarIds == null) {
            selection = null
            selectionArgs = null
        } else {
            selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${calendarIds.joinToString(",") { "?" }})"
            selectionArgs = calendarIds.map { it.toString() }.toTypedArray()
        }

        val events = mutableListOf<CalendarEvent>()
        context.contentResolver.query(
            uriBuilder.build(),
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val beginCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val titleCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val calNameCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val allDayCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val colorCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DISPLAY_COLOR)

            while (cursor.moveToNext()) {
                events += CalendarEvent(
                    eventId = cursor.getLong(idCol),
                    instanceStartMillis = cursor.getLong(beginCol),
                    instanceEndMillis = cursor.getLong(endCol),
                    title = cursor.getString(titleCol) ?: "(No title)",
                    calendarDisplayName = cursor.getString(calNameCol) ?: "",
                    allDay = cursor.getInt(allDayCol) != 0,
                    color = opaqueOrDefault(cursor, colorCol),
                )
            }
        }
        return events
    }

    /** Calendar providers store colors with alpha 0 or not at all; force them opaque. */
    private fun opaqueOrDefault(cursor: android.database.Cursor, col: Int): Int =
        if (cursor.isNull(col)) CalendarEvent.DEFAULT_COLOR else cursor.getInt(col) or 0xFF000000.toInt()

    private companion object {
        const val INITIAL_COUNT_SEARCH_WINDOW_DAYS = 30
    }
}
