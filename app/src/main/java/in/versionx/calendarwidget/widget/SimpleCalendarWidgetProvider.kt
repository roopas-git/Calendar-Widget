package `in`.versionx.calendarwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import `in`.versionx.calendarwidget.MainActivity
import `in`.versionx.calendarwidget.OpenEventActivity
import `in`.versionx.calendarwidget.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Home-screen widget listing upcoming events, with per-instance colors/opacity/
 * corner-radius/text-size (see [WidgetAppearance], configured via
 * [WidgetConfigureActivity]). Content is refreshed by
 * [CalendarCheckWorker][`in`.versionx.calendarwidget.work.CalendarCheckWorker] once a day
 * (and immediately on "Check now" or after saving settings); Android's own onUpdate
 * periodic callback (configured in simple_calendar_widget_info.xml) acts only as a
 * fallback, since the OS does not guarantee updatePeriodMillis will fire on time.
 *
 * The event list itself is rendered by [CalendarWidgetRemoteViewsService] (a
 * RemoteViews collection adapter), since a flat RemoteViews TextView can't give
 * individual rows their own color — needed for the "today" highlight.
 */
class SimpleCalendarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        MidnightRefresh.schedule(context)
    }

    override fun onEnabled(context: Context) {
        MidnightRefresh.schedule(context)
    }

    override fun onDisabled(context: Context) {
        MidnightRefresh.cancel(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach {
            WidgetAppearancePrefs.delete(context, it)
            WidgetCalendarSelectionPrefs.clear(context, it)
        }
    }

    companion object {
        private const val DEFAULT_WIDTH_DP = 250
        private const val DEFAULT_HEIGHT_DP = 150
        private const val MIN_BITMAP_PX = 50
        private const val MAX_BITMAP_PX = 1200
        const val DATE_DAY_SCALE = 2.4f

        /** Called by the worker after each daily/manual check. */
        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SimpleCalendarWidgetProvider::class.java))
            for (id in ids) {
                refreshWidget(context, id, manager)
            }
            if (ids.isNotEmpty()) MidnightRefresh.schedule(context)
        }

        /** Called by [WidgetConfigureActivity] right after saving one widget's settings. */
        fun refreshWidget(context: Context, appWidgetId: Int, manager: AppWidgetManager = AppWidgetManager.getInstance(context)) {
            // Use the application context: on newer Android, setRemoteAdapter binds the list
            // service asynchronously with this context, and the config activity finishes right
            // after saving — binding to it leaks the connection and drops the update.
            updateWidget(context.applicationContext, manager, appWidgetId)
            manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_events_list)
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val appearance = WidgetAppearancePrefs.get(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_simple_calendar)

            views.setImageViewBitmap(
                R.id.widget_background,
                roundedBackgroundBitmap(context, manager, appWidgetId, appearance),
            )

            val today = Calendar.getInstance()
            views.setTextViewText(
                R.id.widget_date_month,
                SimpleDateFormat("MMM", Locale.getDefault()).format(today.time).uppercase(Locale.getDefault()),
            )
            views.setTextViewText(R.id.widget_date_day, today.get(Calendar.DAY_OF_MONTH).toString())
            views.setTextViewText(
                R.id.widget_date_weekday,
                SimpleDateFormat("EEE", Locale.getDefault()).format(today.time).uppercase(Locale.getDefault()),
            )
            views.setViewVisibility(R.id.widget_date_block, if (appearance.showDateBlock) View.VISIBLE else View.GONE)
            views.setTextColor(R.id.widget_date_month, appearance.dateColor)
            views.setTextColor(R.id.widget_date_day, appearance.dateColor)
            views.setTextColor(R.id.widget_date_weekday, appearance.dateColor)
            // The date block scales with the chosen text size, keeping the old app's proportions.
            val smallSp = appearance.textSizeSp.toFloat()
            views.setTextViewTextSize(R.id.widget_date_month, TypedValue.COMPLEX_UNIT_SP, smallSp)
            views.setTextViewTextSize(R.id.widget_date_day, TypedValue.COMPLEX_UNIT_SP, smallSp * DATE_DAY_SCALE)
            views.setTextViewTextSize(R.id.widget_date_weekday, TypedValue.COMPLEX_UNIT_SP, smallSp)

            views.setTextViewText(R.id.widget_empty_text, context.getString(R.string.widget_no_upcoming_events))
            views.setTextColor(R.id.widget_empty_text, appearance.textColor)
            views.setEmptyView(R.id.widget_events_list, R.id.widget_empty_text)

            val adapterIntent = Intent(context, CalendarWidgetRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("widget://$appWidgetId")
            }
            views.setRemoteAdapter(R.id.widget_events_list, adapterIntent)

            val launchIntent = MainActivity.newIntent(context)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Row taps: each row fills in its event; OpenEventActivity opens it in the calendar app.
            // The template must be mutable so the fill-in extras can be added, and explicit
            // (Android 14+ rejects mutable PendingIntents with implicit intents).
            val rowTemplate = PendingIntent.getActivity(
                context,
                appWidgetId,
                Intent(context, OpenEventActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0),
            )
            views.setPendingIntentTemplate(R.id.widget_events_list, rowTemplate)

            manager.updateAppWidget(appWidgetId, views)
        }

        private fun roundedBackgroundBitmap(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
            appearance: WidgetAppearance,
        ): Bitmap {
            val density = context.resources.displayMetrics.density
            val options = manager.getAppWidgetOptions(appWidgetId)
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
                .takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
                .takeIf { it > 0 } ?: DEFAULT_HEIGHT_DP
            val widthPx = (widthDp * density).toInt().coerceIn(MIN_BITMAP_PX, MAX_BITMAP_PX)
            val heightPx = (heightDp * density).toInt().coerceIn(MIN_BITMAP_PX, MAX_BITMAP_PX)

            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val alpha = (appearance.backgroundOpacityPercent * 255 / 100).coerceIn(0, 255)
            fun withAlpha(c: Int) = AndroidColor.argb(alpha, AndroidColor.red(c), AndroidColor.green(c), AndroidColor.blue(c))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = withAlpha(appearance.backgroundColor)
                if (appearance.gradientEnabled) {
                    shader = LinearGradient(
                        0f, 0f, widthPx.toFloat(), heightPx.toFloat(),
                        withAlpha(appearance.backgroundColor),
                        withAlpha(appearance.gradientEndColor),
                        Shader.TileMode.CLAMP,
                    )
                }
            }
            val radiusPx = appearance.cornerRadiusDp * density
            canvas.drawRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), radiusPx, radiusPx, paint)
            return bitmap
        }
    }
}
