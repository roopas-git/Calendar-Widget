package `in`.versionx.calendarwidget.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.versionx.calendarwidget.R
import `in`.versionx.calendarwidget.data.CalendarEvent
import `in`.versionx.calendarwidget.ui.theme.WallpaperGradient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * A Compose mock of the home-screen widget, drawn over a painted-wallpaper
 * gradient, so settings changes can be seen live before saving. Mirrors what
 * [SimpleCalendarWidgetProvider] and [EventLineFormatter.styledLine] render.
 */
@Composable
fun WidgetPreview(
    appearance: WidgetAppearance,
    events: List<CalendarEvent>,
    modifier: Modifier = Modifier,
    maxRows: Int = 5,
    /** True before calendar access is granted, so the preview has something to show. */
    showSamplesWhenEmpty: Boolean = false,
) {
    val samples = remember { sampleEvents() }
    val rows = (if (events.isEmpty() && showSamplesWhenEmpty) samples else events).take(maxRows)
    val alpha = appearance.backgroundOpacityPercent / 100f
    val bg = Color(appearance.backgroundColor).copy(alpha = alpha)
    val brush = if (appearance.gradientEnabled) {
        Brush.linearGradient(listOf(bg, Color(appearance.gradientEndColor).copy(alpha = alpha)))
    } else {
        Brush.linearGradient(listOf(bg, bg))
    }
    val shape = RoundedCornerShape(appearance.cornerRadiusDp.dp)
    val textSp = appearance.textSizeSp.sp
    val today = remember { Calendar.getInstance() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WallpaperGradient)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .clip(shape)
                .background(brush, shape)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (appearance.showDateBlock) {
                Column(
                    modifier = Modifier.padding(end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val dateColor = Color(appearance.dateColor)
                    Text(
                        text = SimpleDateFormat("MMM", Locale.getDefault()).format(today.time).uppercase(),
                        color = dateColor, fontWeight = FontWeight.Bold, fontSize = textSp,
                    )
                    Text(
                        text = today.get(Calendar.DAY_OF_MONTH).toString(),
                        color = dateColor, fontWeight = FontWeight.Bold,
                        fontSize = textSp * SimpleCalendarWidgetProvider.DATE_DAY_SCALE,
                        lineHeight = textSp * SimpleCalendarWidgetProvider.DATE_DAY_SCALE,
                    )
                    Text(
                        text = SimpleDateFormat("EEE", Locale.getDefault()).format(today.time).uppercase(),
                        color = dateColor, fontWeight = FontWeight.Bold, fontSize = textSp,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (rows.isEmpty()) {
                    Text(
                        text = stringResource(R.string.widget_no_upcoming_events),
                        color = Color(appearance.textColor),
                        fontSize = textSp,
                    )
                }
                rows.forEach { event ->
                    val parts = EventLineFormatter.parts(event)
                    val base = Color(EventLineFormatter.rowColor(event, appearance))
                    Text(
                        text = buildAnnotatedString {
                            if (appearance.showCalendarColors) {
                                withStyle(SpanStyle(color = Color(event.color))) { append("● ") }
                            }
                            withStyle(SpanStyle(color = base, fontWeight = FontWeight.Bold)) { append(parts.label) }
                            append("  ")
                            withStyle(SpanStyle(color = base.copy(alpha = base.alpha * 0.85f))) { append(parts.title) }
                        },
                        fontSize = textSp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Shown in the preview before calendar access is granted. */
private fun sampleEvents(): List<CalendarEvent> {
    val now = System.currentTimeMillis()
    val day = TimeUnit.DAYS.toMillis(1)
    fun at(daysAhead: Int, hour: Int): Long = Calendar.getInstance().apply {
        timeInMillis = now + daysAhead * day
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0)
    }.timeInMillis
    fun allDay(daysAhead: Int): Long {
        val local = Calendar.getInstance().apply { timeInMillis = now + daysAhead * day }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
    return listOf(
        CalendarEvent(1, allDay(0), allDay(1), "Asha's Birthday", "", true, 0xFFE91E63.toInt()),
        CalendarEvent(2, at(1, 11), at(1, 12), "Team sync", "", false, 0xFF2196F3.toInt()),
        CalendarEvent(3, at(1, 13), at(1, 14), "Lunch with Ravi", "", false, 0xFF4CAF50.toInt()),
        CalendarEvent(4, at(2, 10), at(2, 11), "Dentist appointment", "", false, 0xFFFF9800.toInt()),
        CalendarEvent(5, allDay(3), allDay(4), "Family trip", "", true, 0xFF9C27B0.toInt()),
    )
}
