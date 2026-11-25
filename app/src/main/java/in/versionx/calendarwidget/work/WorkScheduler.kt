package `in`.versionx.calendarwidget.work

import android.content.Context
import android.provider.CalendarContract
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules [CalendarCheckWorker]:
 * - daily, as a safety net;
 * - whenever anything in the phone's calendar database changes (an event is added,
 *   edited, deleted or synced), so widgets and reminders stay current within a minute.
 * Safe to call repeatedly.
 */
object WorkScheduler {

    private const val UNIQUE_WORK_NAME = "calendar_check_daily"
    private const val UNIQUE_WATCH_NAME = "calendar_check_on_change"
    const val WATCH_TAG = "calendar_watch"

    fun scheduleDailyCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<CalendarCheckWorker>(
            CalendarCheckWorker.REPEAT_INTERVAL,
            CalendarCheckWorker.REPEAT_INTERVAL_UNIT,
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        watchCalendarChanges(context)
    }

    /**
     * Arms a one-shot check that waits for a change under the calendar provider.
     * WorkManager content triggers fire once, so each watch run re-arms the next one
     * ([afterCurrentRun] = true appends it behind the running work instead of
     * replacing it).
     */
    fun watchCalendarChanges(context: Context, afterCurrentRun: Boolean = false) {
        val constraints = Constraints.Builder()
            .addContentUriTrigger(CalendarContract.CONTENT_URI, true)
            .setTriggerContentUpdateDelay(10, TimeUnit.SECONDS)
            .setTriggerContentMaxDelay(1, TimeUnit.MINUTES)
            .build()
        val request = OneTimeWorkRequestBuilder<CalendarCheckWorker>()
            .setConstraints(constraints)
            .addTag(WATCH_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WATCH_NAME,
            if (afterCurrentRun) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** Runs the check once immediately (e.g. "Check now" button), independent of the schedule. */
    fun runOnce(context: Context) {
        val request = OneTimeWorkRequestBuilder<CalendarCheckWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
