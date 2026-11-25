# Permissions

Declared in `app/src/main/AndroidManifest.xml`:

| Permission | Type | Why |
|---|---|---|
| `android.permission.READ_CALENDAR` | Dangerous (runtime) | Read events from every calendar provider on the device (Google Calendar, other synced accounts, local calendars) via `CalendarContract` |
| `android.permission.POST_NOTIFICATIONS` | Dangerous (runtime, Android 13+/API 33 only) | Required since Android 13 to show any notification at all |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal (no prompt) | Lets `BootRescheduleReceiver` re-enqueue the daily check after reboot |

## Request flow (`MainActivity.kt`)

1. On launch, check `READ_CALENDAR`. If missing, show a rationale string
   (`permission_rationale_calendar`) and a "Grant permissions" button.
2. Tapping it launches the system permission dialog for `READ_CALENDAR`.
3. If granted:
   - Query events immediately and show them in the list.
   - Schedule the daily `WorkManager` check.
   - If running on API 33+ and `POST_NOTIFICATIONS` isn't already granted, chain
     into that permission request too.
4. If the user denies either permission, the app degrades gracefully:
   - No `READ_CALENDAR` → `CalendarCheckWorker.doWork()` returns `Result.success()`
     without querying anything (no crash, no retry loop); the widget shows
     "No events in the next 7 days"; the on-screen list stays empty with the
     rationale/button shown again.
   - No `POST_NOTIFICATIONS` → the worker still updates the widget and the in-app
     list, it just skips posting notifications.

## Why no `SCHEDULE_EXACT_ALARM`

The spec is a daily background check, not a reminder that must fire at an exact
moment — `WorkManager`'s inexact periodic scheduling is sufficient and avoids the
extra permission (and Play Store policy scrutiny) that exact alarms require.
