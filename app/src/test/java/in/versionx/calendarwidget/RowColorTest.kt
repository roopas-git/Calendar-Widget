package `in`.versionx.calendarwidget

import `in`.versionx.calendarwidget.data.CalendarEvent
import `in`.versionx.calendarwidget.widget.EventLineFormatter
import `in`.versionx.calendarwidget.widget.WidgetAppearance
import `in`.versionx.calendarwidget.widget.WidgetThemePreset
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Row colors follow the old app: today, tomorrow and later events each get their own color. */
class RowColorTest {

    private lateinit var originalZone: TimeZone
    private val hour = TimeUnit.HOURS.toMillis(1)
    private val appearance = WidgetAppearance.DEFAULT

    @Before fun setUp() {
        originalZone = TimeZone.getDefault()
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

    private val now = local(2026, 11, 25, 16, 0)

    @Test fun defaultsAreTheOldAppColors() {
        assertEquals(0xFF000000.toInt(), appearance.backgroundColor)
        assertEquals(70, appearance.backgroundOpacityPercent)
        assertEquals(0xCCFFFFFF.toInt(), appearance.textColor)
        assertEquals(0xFFFFAB00.toInt(), appearance.todayColor)
        assertEquals(0xFFFFAB00.toInt(), appearance.dateColor)
        assertEquals(0xFF009ACD.toInt(), appearance.tomorrowColor)
        assertTrue(WidgetThemePreset.CLASSIC.matches(appearance))
    }

    @Test fun todayEventUsesTodayColor() =
        assertEquals(appearance.todayColor, EventLineFormatter.rowColor(timed(local(2026, 11, 25, 23, 30)), appearance, now))

    @Test fun tomorrowEventUsesTomorrowColor() =
        assertEquals(appearance.tomorrowColor, EventLineFormatter.rowColor(timed(local(2026, 11, 26, 0, 25)), appearance, now))

    @Test fun allDayTomorrowUsesTomorrowColorWestOfUtc() =
        assertEquals(appearance.tomorrowColor, EventLineFormatter.rowColor(allDay(2026, 11, 26), appearance, now))

    @Test fun laterEventUsesTextColor() =
        assertEquals(appearance.textColor, EventLineFormatter.rowColor(timed(local(2026, 11, 27, 9, 0)), appearance, now))
}
