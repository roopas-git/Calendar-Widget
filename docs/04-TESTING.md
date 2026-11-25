# Manual test plan

No automated tests were added beyond the default project template
(`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`) — calendar/notification/widget
behavior is easiest to verify manually on a device or emulator with a real (or
synced) calendar account. This file is the checklist to run through after any change
to the data/notification/work/widget code.

## Setup

1. On the test device/emulator, add a Google account with a calendar that has a few
   events: at least one **today**, one **3–4 days out**, one **exactly at the edge**
   of 7 days, and one **8+ days out** (should NOT appear).
2. `./gradlew installDebug`, launch the app.

## 1. Permission flow

- [ ] Fresh install, calendar permission not yet granted → rationale text + "Grant
      permissions" button shown, event list empty.
- [ ] Tap "Grant permissions" → system dialog → Allow → event list populates; on
      API 33+, a second system dialog for notifications should follow automatically.
- [ ] Deny calendar permission → app doesn't crash; rationale/button remain visible.
- [ ] Deny notification permission (API 33+) → app doesn't crash; events still show
      on screen and in the widget, just no notifications later.

## 2. Event list correctness

- [ ] Default (Days mode, 7): events shown match exactly today through 7 days
      ahead, sorted soonest first; the 8+ day event does not appear.
- [ ] Events from more than one calendar (if multiple accounts/calendars exist) all
      appear, with the correct `calendarDisplayName`.
- [ ] All-day events display sensibly (no obviously wrong time like midnight UTC
      shifted a day).

## 2b. Lookahead setting (Days vs. Count)

- [ ] "Number of days" chip selected by default, stepper shows "7 days ahead".
- [ ] Tapping "Number of events" switches mode; stepper now shows "N upcoming
      events" (default value carries over — confirm it's still usable, e.g. not 0).
- [ ] In Count mode, the list shows exactly N events regardless of how many days
      out they fall (test with a calendar sparse enough that N events spans more
      than 7 days — confirms the widening-window search in
      `getUpcomingEventsByCount` works, not just the first 30-day window).
- [ ] +/− buttons clamp at 1 and 60 (`LookaheadSettings.MIN_VALUE`/`MAX_VALUE`) —
      don't go below 1 or above 60.
- [ ] Changing mode or value updates the on-screen list immediately (no need to tap
      "Check now" separately).
- [ ] Force-stop and relaunch the app → the chosen mode/value is remembered
      (persisted via `LookaheadPreferences`, backed by SharedPreferences).
- [ ] After changing the setting, check the **widget** (see section 5) picks up the
      same mode/value on its next refresh — not just the in-app list.

## 3. "Check now" button

- [ ] Tapping it re-queries immediately and updates the list without restarting the
      app.
- [ ] Add a new event in Google Calendar, pull/re-check → it appears without
      reinstalling.

## 4. Background check / event reminders

How it works now (see `docs/flow.md` section 8): the check runs daily, within a
minute of any calendar change, and on "Check now". It sets an alarm per event for
**15 minutes before** timed events and **9 AM** for all-day events (only reminders
due in the next 48 hours). Alarms are visible with `adb shell dumpsys alarm | grep
-A3 in.versionx.calendarwidget`.

- [ ] Add an event starting ~20 minutes from now in a calendar a widget shows →
      within ~1 minute the widget shows it, and a reminder alarm appears in
      `dumpsys alarm`.
- [ ] The reminder arrives about 15 minutes before (up to 10 minutes later on
      Android 12+), as "Title" / "Today 11:00 AM · Calendar", with the default sound.
- [ ] An all-day event today reminds at 9 AM, or right away if it's already past 9.
- [ ] Tapping the reminder opens the event in the calendar app.
- [ ] Delete an event before its reminder → no reminder appears.
- [ ] Running "Check now" again does **not** show the same reminder twice.
- [ ] A first run with many upcoming events does **not** produce a burst of
      notifications; only events due within ~48 hours get alarms.
- [ ] Reminders only cover calendars ticked on at least one widget (all calendars
      if no widget is placed).
- [ ] After a reboot, `dumpsys alarm` shows the reminder alarms again.

## 5. Widget

- [ ] Long-press home screen → Widgets → "Simple Calendar Widget" → drag to home
      screen. **`WidgetConfigureActivity` should open automatically before the
      widget is actually placed** — see section 5a before continuing.
- [ ] After saving the configure screen, the widget appears showing the date block
      (today's day/month/weekday) and the event list, scrollable if it overflows
      the widget's height, with no cap on how many events show (all events from the
      current lookahead setting are listed, scrolling takes care of the rest).
- [ ] Tapping the widget's background (outside the list) or a row opens
      `MainActivity`.
- [ ] After "Check now" or the daily worker runs, the widget content refreshes
      without needing to remove/re-add it.
- [ ] Revoke calendar permission (Settings → Apps → Simple Calendar Widget →
      Permissions) → widget shows "No upcoming events" instead of crashing.
- [ ] Resize the widget (drag its handles) → content and background both still
      render sensibly at the new size (corners may look very slightly off-circular
      right at the moment of resize — expected, see `01-ARCHITECTURE.md`'s note on
      the rendered-bitmap background; should look correct again after the next
      refresh).

## 5a. Widget appearance (config activity + color picker)

- [ ] On first placement, the configure screen shows the old app's colors as
      defaults: black background (#000000) at 70% opacity, text #CCFFFFFF,
      today and date color #FFAB00, tomorrow color #009ACD; plus 16dp corner
      radius and 12sp text size. The Classic theme is selected.
- [ ] Tap "Change" next to Background Color → `ColorPickerDialog` opens showing the
      current color. Drag within the saturation/value square and the hue slider →
      the preview swatch and hex value update live.
- [ ] Tap "Pick" → dialog closes, the row's swatch updates to the new color; tap
      "Close" instead on another color row → color is discarded, swatch unchanged.
- [ ] Repeat for Text Color, Today Color, Tomorrow Color and Date Color.
- [ ] Drag the Background Opacity, Round Edges, and Text Size sliders → labels
      update live (e.g. "Round Edges: 24dp").
- [ ] Tap "Save" → activity closes, widget appears/updates on the home screen
      reflecting every change: background color+opacity+corner radius (rendered
      bitmap), text color on non-today rows, today color on today's row(s), and
      the new text size on all rows.
- [ ] Add a **second** widget instance, give it different colors → confirm the
      first widget's appearance is unaffected (settings are per `appWidgetId`, via
      `WidgetAppearancePrefs`).
- [ ] Force-stop and relaunch the launcher/device → both widgets keep their own
      saved appearance.
- [ ] Remove a widget from the home screen, confirm (can check via
      `adb shell cat /data/data/in.versionx.calendarwidget/shared_prefs/widget_appearance.xml`
      on a debuggable/rooted device) that its entries are cleared, not left behind.
- [ ] On Android 12+, long-press the widget → "Edit widget" (or equivalent) should
      reopen the configure screen for that same instance with its current values
      pre-filled, not the defaults.
- [ ] An event starting today should visibly use the Today Color, an event
      tomorrow the Tomorrow Color; events on later days use the regular Text Color.

## 5b. Calendar Accounts picker

Needs a device/emulator with **at least two calendars** (e.g. two Google accounts,
or a Google account plus a local/device calendar) with different events on each, to
actually tell filtering apart from "shows everything anyway."

- [ ] Open the configure screen (place a new widget, or "Edit widget" on an
      existing one) without calendar permission granted → "Calendar access is
      needed to list and choose your calendars." + "Grant calendar permission"
      button shown instead of the calendar list.
- [ ] Grant permission → the calendar list appears, **every calendar checked by
      default** (first time configuring this widget instance).
- [ ] Uncheck one calendar → Save → the widget's event list no longer includes that
      calendar's events. If it's the only widget, the main app list (after "Check
      now") and reminders also drop that calendar — they use the union of all
      widgets' calendars.
- [ ] Uncheck **every** calendar → Save → the widget and preview show "No upcoming
      events" (not every calendar).
- [ ] Reopen the configure screen for that widget → the unchecked calendar shows
      unchecked (persisted via `WidgetCalendarSelectionPrefs`), not reset to
      all-checked.
- [ ] Re-check every calendar and Save → add a new calendar account to the device
      afterward (or just reason about it, if adding one isn't practical) → that
      widget should still show the new calendar's events without reopening its
      configure screen, confirming "all checked" was saved as "no filter"
      (`null`), not a frozen list of the IDs that existed at save time.
- [ ] Add a **second** widget instance with a different calendar selection than the
      first → confirm each widget's event list reflects its own selection
      independently.
- [ ] On a device/emulator with no calendar accounts at all, the configure screen
      should show "No calendars found on this device." instead of an empty list or
      a crash.

## 6. Reboot

- [ ] Reboot the device/emulator, confirm (via Background Task Inspector) the
      periodic work is still enqueued without reopening the app.
