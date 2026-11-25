package `in`.versionx.calendarwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Keeps the widget's date block and "Today" / "Tomorrow" labels correct when the day
 * changes. A non-waking alarm fires a few seconds after local midnight (within a
 * 10-minute window, which needs no exact-alarm permission); if the phone is asleep it
 * fires as soon as the phone wakes, which is when the widget is seen anyway. Clock and
 * time-zone changes trigger the same refresh.
 */
object MidnightRefresh {

    private const val REQUEST_CODE = 4242
    private val WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(10)

    /** Schedules (or re-schedules) the next midnight refresh. Safe to call often. */
    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.setWindow(AlarmManager.RTC, nextMidnight(), WINDOW_MILLIS, pendingIntent(context))
    }

    /** Called when the last widget is removed. */
    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }

    internal fun nextMidnight(now: Long = System.currentTimeMillis()): Long =
        Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 5)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, MidnightRefreshReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Receives the midnight alarm plus system clock / time-zone changes. */
class MidnightRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            null, // our own midnight alarm
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> SimpleCalendarWidgetProvider.refreshAllWidgets(context) // also schedules the next alarm
        }
    }
}
