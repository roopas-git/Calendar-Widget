package `in`.versionx.calendarwidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import `in`.versionx.calendarwidget.OpenEventActivity
import `in`.versionx.calendarwidget.R
import `in`.versionx.calendarwidget.data.CalendarEvent
import `in`.versionx.calendarwidget.data.CalendarRepository
import `in`.versionx.calendarwidget.data.LookaheadPreferences

/**
 * Backs the widget's event list so each row can get its own text color (today's
 * events vs. the rest) — a flat RemoteViews TextView can only have one color for
 * the whole block, which isn't enough to support the old app's "Today Color"
 * setting.
 */
class CalendarWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        return CalendarRemoteViewsFactory(applicationContext, appWidgetId)
    }
}

private class CalendarRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {

    private var events: List<CalendarEvent> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        events = try {
            val calendarIds = WidgetCalendarSelectionPrefs.get(context, appWidgetId)
            CalendarRepository(context).getUpcomingEvents(LookaheadPreferences.get(context), calendarIds = calendarIds)
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    override fun onDestroy() {
        events = emptyList()
    }

    override fun getCount(): Int = events.size

    override fun getViewAt(position: Int): RemoteViews {
        val event = events[position]
        val appearance = WidgetAppearancePrefs.get(context, appWidgetId)
        val views = RemoteViews(context.packageName, R.layout.widget_event_row)

        views.setTextViewText(R.id.event_row_text, EventLineFormatter.styledLine(event, appearance))
        views.setTextViewTextSize(R.id.event_row_text, TypedValue.COMPLEX_UNIT_SP, appearance.textSizeSp.toFloat())
        // Tapping a row opens this event in the calendar app (see OpenEventActivity).
        views.setOnClickFillInIntent(R.id.event_row_text, OpenEventActivity.fillIn(event))

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}
