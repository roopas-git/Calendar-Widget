package `in`.versionx.calendarwidget.notification

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import `in`.versionx.calendarwidget.data.CalendarEvent
import `in`.versionx.calendarwidget.data.CalendarRepository
import java.util.concurrent.TimeUnit

/**
 * Schedules one alarm per upcoming event, firing at [ReminderTimes.scheduleTime]
 * (15 minutes before a timed event, 9 AM on an all-day event's day). Called from the
 * background check, which runs daily and whenever the calendar changes, so every
 * event gets scheduled well before its reminder is due.
 *
 * Re-scheduling the same occurrence replaces its alarm (same PendingIntent), and
 * [NotifiedEventStore] makes sure it's only ever shown once.
 */
object EventReminders {

    /** Window for inexact alarms on Android 12+ (no exact-alarm permission needed). */
    private val WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(10)

    fun scheduleUpcoming(context: Context, calendarIds: Set<Long>?, now: Long = System.currentTimeMillis()) {
        val store = NotifiedEventStore(context)
        val days = TimeUnit.MILLISECONDS.toDays(ReminderTimes.SCHEDULE_HORIZON_MILLIS).toInt() + 1
        val events = CalendarRepository(context).getUpcomingEventsByDays(days, now, calendarIds)
        for (event in events) {
            if (store.hasBeenNotified(event)) continue
            val at = ReminderTimes.scheduleTime(event, now) ?: continue
            schedule(context, event, at)
        }
    }

    private fun schedule(context: Context, event: CalendarEvent, at: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            event.instanceKey.hashCode(),
            EventReminderReceiver.intent(context, event),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MILLIS, pi)
        }
    }
}

/** Fires at reminder time: re-checks the event still exists, then posts the notification. */
class EventReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = eventFrom(intent) ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        // Skip events deleted or moved since the alarm was set (a moved event gets its own alarm).
        if (!CalendarRepository(context).instanceExists(event.eventId, event.instanceStartMillis)) return

        val store = NotifiedEventStore(context)
        if (store.hasBeenNotified(event)) return
        NotificationHelper.ensureChannel(context)
        if (NotificationHelper.notifyEvent(context, event)) {
            store.markNotified(event)
        }
    }

    companion object {
        private const val EXTRA_EVENT_ID = "event_id"
        private const val EXTRA_BEGIN = "begin"
        private const val EXTRA_END = "end"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_CALENDAR = "calendar"
        private const val EXTRA_ALL_DAY = "all_day"
        private const val EXTRA_COLOR = "color"

        fun intent(context: Context, event: CalendarEvent): Intent =
            Intent(context, EventReminderReceiver::class.java)
                // A distinct data URI keeps each occurrence's PendingIntent separate.
                .setData(Uri.parse("reminder://${event.instanceKey}"))
                .putExtra(EXTRA_EVENT_ID, event.eventId)
                .putExtra(EXTRA_BEGIN, event.instanceStartMillis)
                .putExtra(EXTRA_END, event.instanceEndMillis)
                .putExtra(EXTRA_TITLE, event.title)
                .putExtra(EXTRA_CALENDAR, event.calendarDisplayName)
                .putExtra(EXTRA_ALL_DAY, event.allDay)
                .putExtra(EXTRA_COLOR, event.color)

        private fun eventFrom(intent: Intent): CalendarEvent? {
            val id = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
            if (id < 0) return null
            return CalendarEvent(
                eventId = id,
                instanceStartMillis = intent.getLongExtra(EXTRA_BEGIN, 0L),
                instanceEndMillis = intent.getLongExtra(EXTRA_END, 0L),
                title = intent.getStringExtra(EXTRA_TITLE) ?: "",
                calendarDisplayName = intent.getStringExtra(EXTRA_CALENDAR) ?: "",
                allDay = intent.getBooleanExtra(EXTRA_ALL_DAY, false),
                color = intent.getIntExtra(EXTRA_COLOR, CalendarEvent.DEFAULT_COLOR),
            )
        }
    }
}
