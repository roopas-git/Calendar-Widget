package `in`.versionx.calendarwidget.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * After a reboot: alarms (reminders, the midnight widget refresh) are wiped by
 * Android, so run the check once to re-schedule them and refresh widgets. Also
 * re-enqueues the daily work (KEEP policy, a no-op if it still exists) as a safety
 * net for OEM battery managers that clear WorkManager's own bookkeeping.
 */
class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            WorkScheduler.scheduleDailyCheck(context)
            WorkScheduler.runOnce(context)
        }
    }
}
