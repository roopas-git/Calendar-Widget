# Publishing to the Play Store as a new app

## Decision (2026-11-24)

This project is released as a **new Play Store app**, not as an update to the old
listing.

- New `applicationId`: `in.versionx.calendarwidget`
- Old listing (`com.mondaz.widgettest`) is not being updated. Its signing keystore was
  lost, and that is why we stopped trying to update it.

Consequences of a new listing:

- Existing users of the old app do not get this build as an update. They must install
  it separately, and their old settings and widgets do not carry over.
- Reviews, ratings and install count start from zero.
- Because the package name is new, there is no signing-key constraint: any new
  upload key works.

## Signing: create the upload keystore once and keep it safe

```bash
keytool -genkeypair -v \
  -keystore simple-calendar-widget-upload.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 9125
```

Store the `.jks` file and its passwords somewhere durable and backed up (password
manager + an offline copy). Losing the keystore is what ended the old listing. Never
commit it to the repo.

When creating the app in Play Console, keep **Play App Signing** enabled (the
default). Google then holds the app signing key, and a lost *upload* key can be reset
through Play Console support instead of forcing another new listing.

## Versioning

`versionCode` (`app/build.gradle.kts` → `defaultConfig.versionCode`) can start at 1
for a new app. Every later upload must have a strictly greater `versionCode` than the
last one uploaded.
