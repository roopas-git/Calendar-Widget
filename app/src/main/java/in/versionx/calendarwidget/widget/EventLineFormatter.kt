package `in`.versionx.calendarwidget.widget

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import `in`.versionx.calendarwidget.data.CalendarEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Formats one widget row to match the old app's style observed in
 * docs/05-OLD-APP-REFERENCE.md: "Today"/"Tomorrow" for the next two days, then
 * "DD MMM"; a time only on non-all-day events. The date label is bold and the title
 * slightly dimmer, as in the old widget.
 */
object EventLineFormatter {

    /** Alpha applied to event titles so the bold date labels stand out. */
    private const val TITLE_ALPHA = 0xD9

    data class Parts(val label: String, val title: String)

    fun isToday(event: CalendarEvent, now: Long = System.currentTimeMillis()): Boolean =
        daysBetween(now, eventDayStart(event)) == 0

    fun isTomorrow(event: CalendarEvent, now: Long = System.currentTimeMillis()): Boolean =
        daysBetween(now, eventDayStart(event)) == 1

    /** Row color, as in the old app: today's color, tomorrow's color, or the normal text color. */
    fun rowColor(event: CalendarEvent, appearance: WidgetAppearance, now: Long = System.currentTimeMillis()): Int =
        when (daysBetween(now, eventDayStart(event))) {
            0 -> appearance.todayColor
            1 -> appearance.tomorrowColor
            else -> appearance.textColor
        }

    fun parts(event: CalendarEvent, now: Long = System.currentTimeMillis()): Parts {
        val day = dateLabel(event, now)
        val label = if (event.allDay) {
            day
        } else {
            "$day ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(event.instanceStartMillis))}"
        }
        return Parts(label, event.title)
    }

    fun formatLine(event: CalendarEvent, now: Long = System.currentTimeMillis()): String {
        val p = parts(event, now)
        return "${p.label}  ${p.title}"
    }

    /** Styled row text for the widget: optional calendar-color dot, bold label, dimmer title. */
    fun styledLine(
        event: CalendarEvent,
        appearance: WidgetAppearance,
        now: Long = System.currentTimeMillis(),
    ): CharSequence {
        val p = parts(event, now)
        val labelColor = rowColor(event, appearance, now)
        // Title slightly dimmer than the label, but never more opaque than the chosen color.
        val titleAlpha = Color.alpha(labelColor) * TITLE_ALPHA / 0xFF
        val titleColor = Color.argb(titleAlpha, Color.red(labelColor), Color.green(labelColor), Color.blue(labelColor))

        val sb = SpannableStringBuilder()
        if (appearance.showCalendarColors) {
            sb.appendStyled("● ", ForegroundColorSpan(event.color))
        }
        sb.appendStyled(p.label, ForegroundColorSpan(labelColor), StyleSpan(Typeface.BOLD))
        sb.append("  ")
        sb.appendStyled(p.title, ForegroundColorSpan(titleColor))
        return sb
    }

    private fun SpannableStringBuilder.appendStyled(text: String, vararg spans: Any) {
        val start = length
        append(text)
        spans.forEach { setSpan(it, start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
    }

    private fun dateLabel(event: CalendarEvent, now: Long): String = when (daysBetween(now, eventDayStart(event))) {
        0 -> "Today"
        1 -> "Tomorrow"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(eventDayStart(event)))
    }

    /**
     * Local midnight of the day the event starts. All-day events are stored at UTC
     * midnight, so in time zones west of UTC they'd land on the previous local day;
     * they're re-read as a plain local date.
     */
    fun eventDayStart(event: CalendarEvent): Long {
        if (!event.allDay) return event.instanceStartMillis
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = event.instanceStartMillis }
        return Calendar.getInstance().apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    private fun daysBetween(fromMillis: Long, toMillis: Long): Int {
        fun startOfDay(millis: Long) = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val diff = startOfDay(toMillis) - startOfDay(fromMillis)
        return Math.round(diff / TimeUnit.DAYS.toMillis(1).toDouble()).toInt()
    }
}
