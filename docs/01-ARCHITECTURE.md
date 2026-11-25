# Architecture

Plain Android (Kotlin + Jetpack Compose UI), no DI framework, no database — the app
is small enough that SharedPreferences and direct `ContentResolver` queries are
sufficient. If the feature set grows significantly, revisit (e.g. Room for notified-
event tracking, a ViewModel layer for the UI).

## Components

| Layer | File | Responsibility |
|---|---|---|
| Data | `data/CalendarEvent.kt` | Immutable model for one event *occurrence* (an instance, not the recurring series) |
| Data | `data/CalendarInfo.kt` | Immutable model for one device calendar (id, display name, account name) |
| Data | `data/CalendarRepository.kt` | Queries `CalendarContract.Instances` for events (by day range or event count, optionally restricted to a set of calendar IDs) and `CalendarContract.Calendars` for the list of available calendars |
| Data | `data/LookaheadSettings.kt` | `LookaheadMode` (`DAYS`/`COUNT`) + value, persisted via `LookaheadPreferences`; the single source of truth for "how far ahead" shared by the list, worker, and widget |
| Notifications | `notification/NotificationHelper.kt` | Creates the notification channel, builds/posts one notification per event |
| Notifications | `notification/NotifiedEventStore.kt` | SharedPreferences-backed set of "already notified" event occurrences, so the daily check doesn't re-notify the same event every day |
| Background work | `work/CalendarCheckWorker.kt` | `CoroutineWorker`: reads events, notifies new ones, tells the widget to refresh |
| Background work | `work/WorkScheduler.kt` | Enqueues the daily `PeriodicWorkRequest` (and an immediate one-off for "Check now") |
| Background work | `work/BootRescheduleReceiver.kt` | Re-enqueues the periodic work after device reboot, as a safety net |
| Widget | `widget/SimpleCalendarWidgetProvider.kt` | `AppWidgetProvider`; builds the date block, the rounded background bitmap, and wires the event list's `RemoteViewsService` adapter, per widget instance |
| Widget | `widget/CalendarWidgetRemoteViewsService.kt` | `RemoteViewsService` + `RemoteViewsFactory` backing the widget's event `ListView`, so each row can have its own text color (today vs. the rest) |
| Widget | `widget/EventLineFormatter.kt` | Formats one event row as `Today`/`Tomorrow`/`DD MMM` (+ time if not all-day), matching the old app's style |
| Widget | `widget/WidgetAppearance.kt` | Per-widget-instance appearance: background/text/today/tomorrow/date color, background opacity, corner radius, text size, with defaults |
| Widget | `widget/WidgetAppearancePrefs.kt` | SharedPreferences store for `WidgetAppearance`, keyed by `appWidgetId` |
| Widget | `widget/WidgetCalendarSelectionPrefs.kt` | SharedPreferences store for which calendar IDs feed one widget instance (`null` = every calendar), keyed by `appWidgetId` |
| Widget | `widget/WidgetConfigureActivity.kt` | The widget's `configure` activity — lets the user pick which calendars feed this widget and set its `WidgetAppearance`, launched automatically on placement |
| Widget | `widget/ColorPickerDialog.kt` | Reusable HSV (hue-slider + saturation/value square) color picker dialog, used three times in the configure screen |
| UI | `MainActivity.kt` | Compose screen: permission requests, lookahead settings panel, "Check now" button, list of upcoming events |

## Data flow

```
CalendarCheckWorker (runs once/day via WorkManager)
        │
        ├─ LookaheadPreferences.get(context)  → LookaheadSettings(mode, value)
        │
        ├─ CalendarRepository.getUpcomingEvents(settings)
        │        ├─ DAYS mode  → queries CalendarContract.Instances [now, now+Nd]
        │        └─ COUNT mode → queries a widening window until N events are found, takes first N
        │
        ├─ for each event not in NotifiedEventStore:
        │        NotificationHelper.notifyEvent(...)   → posts notification, default sound
        │        NotifiedEventStore.markNotified(...)
        │
        └─ SimpleCalendarWidgetProvider.refreshAllWidgets(context)
                 for each placed widget instance:
                   ├─ WidgetAppearancePrefs.get(appWidgetId)  → WidgetAppearance
                   ├─ render date block + rounded background Bitmap, set on the RemoteViews
                   ├─ setRemoteAdapter(→ CalendarWidgetRemoteViewsService, with appWidgetId)
                   └─ notifyAppWidgetViewDataChanged(appWidgetId, widget_events_list)
                            └─ RemoteViewsFactory.onDataSetChanged():
                                 WidgetCalendarSelectionPrefs.get(appWidgetId) → calendarIds (or null = all)
                                 CalendarRepository.getUpcomingEvents(settings, calendarIds = calendarIds)
                                 getViewAt() builds one row per event via EventLineFormatter,
                                 colored todayColor/textColor per WidgetAppearance
```

Note the widget's `RemoteViewsFactory` re-queries events itself (with its own
per-widget calendar filter) rather than reusing the worker's query — the worker's
own `getUpcomingEvents(settings)` call (for notifications and the in-app list) is
always unfiltered by calendar, since calendar selection is a per-widget concept
(see "Design choice" below), not something notifications have a sane per-instance
scope for.

`MainActivity` performs the same `CalendarRepository` query (using the same
`LookaheadSettings`, and likewise no calendar filter — `calendarIds = null`) for its
on-screen list, and triggers `WorkScheduler.scheduleDailyCheck()` once calendar
permission is granted (idempotent — uses `enqueueUniquePeriodicWork` with `KEEP`
policy). Changing the lookahead mode or value in the UI immediately persists it via
`LookaheadPreferences.set()`, re-queries the list, and runs the worker once
(`WorkScheduler.runOnce()`) so the widget and notifications reflect the new setting
without waiting for the next scheduled run.

## Why `CalendarContract.Instances` and not the Events table

`Instances` expands recurring events into their concrete occurrences within a time
range and already does the range filtering server-side (the URI is built with
`[begin, end]` millis appended), which is exactly "events in the next N days" —
including recurring meetings. Querying `Events` directly would require manually
expanding recurrence rules.

Count-based lookahead (`getUpcomingEventsByCount`) still has to go through the same
`Instances` range query, since the API requires an explicit end bound — there's no
"give me the next N events, whatever the range" query. It works around this by
querying a window (30 days), doubling it (60, 120, 240, up to a 365-day cap) until
either enough events are found or the cap is hit, then takes the first N. For a
personal calendar this resolves in one query almost always; the cap exists so a
near-empty calendar can't make this loop run (and query the provider) indefinitely.

The optional calendar filter (`calendarIds`) is applied as a plain SQL `IN (...)`
clause on `Instances.CALENDAR_ID`, passed as `selection`/`selectionArgs` to the same
`ContentResolver.query()` call — no separate code path for "filtered" vs.
"unfiltered," it's just a `null` selection when `calendarIds` is `null` or empty.

## Why WorkManager (not AlarmManager / foreground service)

A once-a-day check with no hard-realtime deadline is exactly what
`PeriodicWorkRequest` is for: it survives process death and (mostly) reboot, respects
Doze/battery optimizations instead of fighting them, and needs no foreground
notification of its own. The 15-minute window: Android computes a 24h window of
possible windows for the periodic work.

The widget's own `updatePeriodMillis` (in `simple_calendar_widget_info.xml`, 6h) is
only a fallback — the OS doesn't guarantee it fires on time. `CalendarCheckWorker`
calls `SimpleCalendarWidgetProvider.refreshAllWidgets()` directly after each check so
the widget refreshes promptly without waiting on that fallback.

## Per-widget appearance and calendar selection: config activity + RemoteViewsService + a rendered background bitmap

Added 2026-11-16 to cover the old app's "Calendar Accounts" picker and its
color/opacity/corner-radius/text-size customization (see
`docs/05-OLD-APP-REFERENCE.md` for the screenshot/description evidence this is
based on). Four things had to be solved, three for the same underlying reason:
`RemoteViews` is a restricted, cross-process view description — no custom `View`
subclasses, no arbitrary `Drawable`s, no access to the widget's own composition
tree the way a normal Activity's UI has.

1. **Per-instance settings, not one global setting.** Two widgets on the same home
   screen can look different (or show different calendars), so both appearance
   (`WidgetAppearancePrefs`) and calendar selection
   (`WidgetCalendarSelectionPrefs`) are keyed by `appWidgetId`, not a single
   app-wide SharedPreferences entry like `LookaheadSettings`. The settings UI is
   the widget's `configure` activity (`WidgetConfigureActivity`, declared via
   `android:configure` in `simple_calendar_widget_info.xml`) — Android launches it
   automatically right after the widget is placed, and (on API 31+, via
   `android:widgetFeatures="reconfigurable"`) again later through the home
   screen's "Edit widget" action. That activity needs `READ_CALENDAR` itself (to
   list calendars to choose from), so it carries its own copy of the
   permission-request flow `MainActivity` already has.
2. **Per-row color ("today" vs. the rest) needs a collection widget.** A single
   `RemoteViews` `TextView` can only have one `textColor` for its whole content —
   not enough to make today's events a different color from the rest. The fix is
   the same mechanism Android's own stack/list widgets use: a `ListView` whose
   adapter is backed by a `RemoteViewsService`
   (`CalendarWidgetRemoteViewsService` + its `RemoteViewsFactory`), which builds one
   independent `RemoteViews` per row and can color each one differently. This same
   factory is also where the per-widget calendar filter is applied, since it's the
   thing that actually queries events for that widget instance.
3. **Arbitrary rounded corners need a rendered bitmap, not a drawable.** There's no
   `RemoteViews` call that sets a runtime corner radius on a background. Instead,
   `SimpleCalendarWidgetProvider.roundedBackgroundBitmap()` draws a
   `Canvas.drawRoundRect()` directly — color, alpha (from the opacity %), and
   radius all baked in — into a `Bitmap` sized to roughly match the widget's
   current dimensions (`AppWidgetManager.getAppWidgetOptions()`), and displays it
   via a full-size `ImageView` (`scaleType="fitXY"`) placed behind the date block
   and event list in a `FrameLayout`. Known trade-off: since the bitmap is
   generated at an approximate rather than exact pixel size, resizing the widget
   can leave the corners looking very slightly off-circular until the next refresh
   re-renders the bitmap at the new size.
4. **"Selected everything" must not freeze the calendar list.** See
   `docs/05-OLD-APP-REFERENCE.md`'s "Calendar selection" section — saving with
   every checkbox checked stores `null` (no filter), not literally every current
   calendar ID, so a calendar account added later still shows up without the user
   having to revisit this screen.

## Known simplifications (acceptable for "simple app" scope)

- Notified-event tracking is SharedPreferences, not a database — fine for the
  volume of events a personal calendar check produces; pruned on every run.
- No ViewModel/state-holder layer in `MainActivity` — state is small enough to live
  directly in the composable with `remember`.
- No retry/backoff policy configured on the `WorkRequest` — default WorkManager
  behavior (exponential backoff) applies if `doWork()` ever returns `Result.retry()`,
  which the current implementation never does since it treats missing permission as
  a terminal (not retryable) state.
- `RemoteViews.setRemoteAdapter(int, Intent)` and
  `AppWidgetManager.notifyAppWidgetViewDataChanged(int, int)` are both flagged
  deprecated (superseded by overloads that assume a newer, more restrictive
  collection-widget binding path added in later API levels). Left as-is: they still
  work correctly at this app's `minSdk`/`targetSdk`, and the replacement APIs would
  add complexity without a behavior change worth it for this app's scope.
- `ColorPickerDialog` is a hue-slider + saturation/value square, not the old app's
  hue-ring + disc — see `docs/05-OLD-APP-REFERENCE.md`'s "The color picker" section
  for why, and how to swap it for a wheel-shaped library later if exact visual
  parity ever matters more than avoiding the extra dependency.
