package `in`.versionx.calendarwidget.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import `in`.versionx.calendarwidget.notification.EventReminders
import `in`.versionx.calendarwidget.notification.NotificationHelper
import `in`.versionx.calendarwidget.notification.NotifiedEventStore
import `in`.versionx.calendarwidget.widget.SimpleCalendarWidgetProvider
import `in`.versionx.calendarwidget.widget.WidgetCalendarSelectionPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * The background check. Runs daily, on "Check now", and within a minute of any
 * calendar change (see [WorkScheduler]). It:
 * 1. schedules reminders for events in the next 48 hours (shown 15 minutes before
 *    timed events, 9 AM for all-day ones — see [EventReminders]), limited to the
 *    calendars the user's widgets show;
 * 2. refreshes every home-screen widget.
 */
class CalendarCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val appContext = applicationContext

        // Re-arm the "calendar changed" trigger: behind this run if this run *is* that trigger.
        WorkScheduler.watchCalendarChanges(appContext, afterCurrentRun = tags.contains(WorkScheduler.WATCH_TAG))

        if (!hasCalendarPermission(appContext)) {
            // Nothing we can do until the user grants it.
            return@withContext Result.success()
        }

        if (NotificationHelper.hasPermission(appContext)) {
            NotificationHelper.ensureChannel(appContext)
            NotifiedEventStore(appContext).pruneOlderThan(System.currentTimeMillis())
            EventReminders.scheduleUpcoming(appContext, WidgetCalendarSelectionPrefs.appCalendarFilter(appContext))
        }

        SimpleCalendarWidgetProvider.refreshAllWidgets(appContext)

        Result.success()
    }

    private fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        val REPEAT_INTERVAL: Long = 24
        val REPEAT_INTERVAL_UNIT: TimeUnit = TimeUnit.HOURS
    }
}
