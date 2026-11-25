package `in`.versionx.calendarwidget

import `in`.versionx.calendarwidget.data.CalendarEvent
import `in`.versionx.calendarwidget.notification.ReminderTimes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class ReminderTimesTest {

    private lateinit var originalZone: TimeZone
    private val minute = TimeUnit.MINUTES.toMillis(1)
    private val hour = TimeUnit.HOURS.toMillis(1)

    @Before fun setUp() {
        originalZone = TimeZone.getDefault()
        // A zone west of UTC, where all-day events (stored at UTC midnight) are easiest to get wrong.
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
    }

    @After fun tearDown() = TimeZone.setDefault(originalZone)

    private fun local(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0) = Calendar.getInstance().apply {
        clear(); set(y, m - 1, d, h, min)
    }.timeInMillis

    private fun utcMidnight(y: Int, m: Int, d: Int) = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(y, m - 1, d)
    }.timeInMillis

    private fun timed(start: Long) = CalendarEvent(1, start, start + hour, "Meeting", "Work", false)
    private fun allDay(y: Int, m: Int, d: Int) =
        CalendarEvent(2, utcMidnight(y, m, d), utcMidnight(y, m, d + 1), "Birthday", "Personal", true)

    @Test fun timedEventRemindsFifteenMinutesBefore() {
        val start = local(2026, 5, 30, 11, 0)
        assertEquals(start - 15 * minute, ReminderTimes.notifyAt(timed(start)))
    }

    @Test fun allDayEventRemindsAtNineOnItsLocalDay() {
        assertEquals(local(2026, 5, 30, 9, 0), ReminderTimes.notifyAt(allDay(2026, 5, 30)))
    }

    @Test fun lateButNotStartedTimedEventRemindsNow() {
        val start = local(2026, 5, 30, 11, 0)
        val now = start - 5 * minute
        assertEquals(now, ReminderTimes.scheduleTime(timed(start), now))
    }

    @Test fun startedTimedEventIsSkipped() {
        val start = local(2026, 5, 30, 11, 0)
        assertNull(ReminderTimes.scheduleTime(timed(start), start + minute))
    }

    @Test fun allDayEventLaterThatDayStillReminds() {
        val now = local(2026, 5, 30, 14, 0)
        assertEquals(now, ReminderTimes.scheduleTime(allDay(2026, 5, 30), now))
    }

    @Test fun allDayEventIsSkippedTheNextDay() {
        assertNull(ReminderTimes.scheduleTime(allDay(2026, 5, 30), local(2026, 5, 31, 0, 1)))
    }

    @Test fun eventsBeyondTheHorizonAreLeftForALaterCheck() {
        val now = local(2026, 5, 30, 8, 0)
        assertNull(ReminderTimes.scheduleTime(timed(now + 72 * hour), now))
    }
}
