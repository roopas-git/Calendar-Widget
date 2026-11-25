package `in`.versionx.calendarwidget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import `in`.versionx.calendarwidget.data.CalendarEvent

/**
 * Invisible hop that opens one event in the phone's calendar app. Widget rows and
 * notifications point here (an explicit intent, which Android requires for the
 * widget's fill-in click template) and this forwards to the calendar app. Falls back
 * to the main screen if no calendar app can show the event.
 */
class OpenEventActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        if (eventId >= 0) {
            val view = Intent(Intent.ACTION_VIEW)
                .setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId))
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, intent.getLongExtra(EXTRA_BEGIN, 0L))
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, intent.getLongExtra(EXTRA_END, 0L))
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, intent.getBooleanExtra(EXTRA_ALL_DAY, false))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(view)
            } catch (e: ActivityNotFoundException) {
                startActivity(MainActivity.newIntent(this))
            }
        } else {
            startActivity(MainActivity.newIntent(this))
        }
        finish()
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_BEGIN = "begin"
        const val EXTRA_END = "end"
        const val EXTRA_ALL_DAY = "all_day"

        /** Extras identifying [event]; used both for direct intents and widget fill-ins. */
        fun fillIn(event: CalendarEvent): Intent = Intent()
            .putExtra(EXTRA_EVENT_ID, event.eventId)
            .putExtra(EXTRA_BEGIN, event.instanceStartMillis)
            .putExtra(EXTRA_END, event.instanceEndMillis)
            .putExtra(EXTRA_ALL_DAY, event.allDay)

        fun newIntent(context: Context, event: CalendarEvent): Intent =
            fillIn(event).setClass(context, OpenEventActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
    }
}
