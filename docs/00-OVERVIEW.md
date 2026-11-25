# Simple Calendar Widget — Overview

## Why this project exists

An older app with this name (package `com.mondaz.widgettest`) is published on the
Play Store, but its source code and signing keystore are lost. This project recreates
it from scratch and, as of 2026-11-24, will be released as a **new Play Store app**,
not as an update to the old listing.

- **Package name (`applicationId`)**: `in.versionx.calendarwidget`
- **App name**: `Simple Calendar Widget`

See `docs/03-PLAYSTORE-RELEASE.md` for the release and signing plan.

## Feature scope — original request vs. the real old app

The project started from a plain-text description (confirmed with the user on
2026-11-03): read events from all device calendars, look 7 days ahead, notify
daily with the default sound, and show a home-screen widget. That's what's
currently implemented (see `01-ARCHITECTURE.md`).

Since then, the user supplied **screenshots and the original Play Store
description** of the actual old app ("CalWidget"), which is a more specific,
configurable product than the plain-text description implied. Per
`docs/05-OLD-APP-REFERENCE.md`, the real app is a **configurable widget**:

1. Reads events from **any synced calendar provider** (Google Calendar, Facebook,
   other social calendars) via `CalendarContract` — already matched by the current
   `CalendarRepository`, since that provider is shared across all sync adapters.
2. Lets the user **pick which specific calendar(s)** feed the widget, via a
   "Calendar Accounts" setting — not an automatic "every calendar" aggregation.
3. Shows a **fixed date block** (day/month/weekday) beside a **scrolling event
   list**, with `Today` / `Tomorrow` / `DD MMM` date labels and times only on
   timed (non-all-day) events.
4. Is customizable: **background color, text color, "today" color, background
   opacity, corner radius ("round edges"), text/font size, and number of upcoming
   events shown** — all via a settings/config screen and HSV color-picker dialogs.
5. Notifications are **not confirmed** to be part of the old app at all — none of
   the screenshots or the description mention them. The notification feature
   currently implemented may be a genuine addition beyond the original app, not a
   replication of it.

This feature list **supersedes** the numbered list above wherever they conflict.
`docs/05-OLD-APP-REFERENCE.md` has the full screenshot-by-screenshot analysis and a
reconciliation table against what's actually built so far, plus open questions
(exact "Display" section contents, count-based vs. day-based event limits,
notification provenance) still blocking a full rebuild.

## Where to read next

- `docs/05-OLD-APP-REFERENCE.md` — **read this one first.** The real feature list,
  screenshot analysis, and open questions blocking a full rebuild.
- `docs/01-ARCHITECTURE.md` — components, data flow, file map for what's
  implemented *so far* (predates the screenshot/description findings above).
- `docs/02-PERMISSIONS.md` — what's requested, when, and why.
- `docs/03-PLAYSTORE-RELEASE.md` — releasing as a new app: upload keystore and
  versioning.
- `docs/04-TESTING.md` — how to manually verify each feature on a device/emulator.
