package `in`.versionx.calendarwidget.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import `in`.versionx.calendarwidget.OpenEventActivity
import `in`.versionx.calendarwidget.R
import `in`.versionx.calendarwidget.data.CalendarEvent
import `in`.versionx.calendarwidget.widget.EventLineFormatter

/**
 * Creates the notification channel and posts one reminder per event. The channel's
 * importance is HIGH so it both pops up and plays the device's default notification
 * sound; no custom sound resource is bundled.
 */
object NotificationHelper {

    const val CHANNEL_ID = "upcoming_events"
    private const val NOTIFICATION_ID_BASE = 1000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            // No setSound() call: this keeps the channel's default sound/vibration,
            // i.e. the device's default notification sound.
        }
        manager.createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Posts a reminder for [event], e.g. title "Team sync", text "Today 11:00 AM · Work".
     * Tapping it opens the event in the calendar app. Returns false (and posts nothing)
     * if notification permission isn't granted.
     */
    fun notifyEvent(context: Context, event: CalendarEvent): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val whenText = EventLineFormatter.parts(event).label
        val text = if (event.calendarDisplayName.isNotBlank()) "$whenText · ${event.calendarDisplayName}" else whenText

        val contentIntent = PendingIntent.getActivity(
            context,
            event.instanceKey.hashCode(),
            OpenEventActivity.newIntent(context, event),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(event.color)
            .setContentTitle(event.title)
            .setContentText(text)
            .setWhen(event.instanceStartMillis)
            .setShowWhen(!event.allDay)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        val notificationId = NOTIFICATION_ID_BASE + (event.instanceKey.hashCode() and 0x7FFFFFFF) % 100000
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }
}
