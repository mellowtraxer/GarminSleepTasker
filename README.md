# Garmin Sleep for Tasker

Android-/Tasker-Plugin, das die neueste Garmin-Schlafsession direkt aus Health Connect liest.

## Version 0.1
- Filtert Schlafsessions explizit auf `com.garmin.android.apps.connectmobile`.
- Summiert Light, Deep, REM, Awake/Awake-in-bed/Out-of-bed und unklassifizierten Schlaf.
- Liest Garmin-Herzfrequenz, SpO2 und Atemfrequenz im exakten Schlafzeitraum, sofern Garmin diese Datentypen in Health Connect bereitstellt.
- Gibt fertige Tasker-Variablen sowie `%calendar_text` zurück.
- Keine Cloud, kein Login, kein Upload.

## Tasker-Ausgaben
`%start_ms`, `%end_ms`, `%total_min`, `%light_min`, `%deep_min`, `%rem_min`, `%awake_min`, `%sleeping_min`, `%avg_hr`, `%avg_spo2`, `%avg_resp`, `%source`, `%calendar_text`.

## Installation / Build
1. Projekt in Android Studio öffnen (JDK 17).
2. Gradle synchronisieren.
3. `app` als Debug APK bauen/installieren.
4. App einmal öffnen und Health-Connect-Berechtigungen erteilen.
5. Mit **Garmin-Schlaf testen** prüfen.
6. In Tasker: Aktion → Plugin → **Garmin-Schlaf auslesen**.

## Wichtiger Hinweis zum Sleep Score
Health Connect definiert im `SleepSessionRecord` keinen standardisierten Garmin Sleep Score. Das Plugin erfindet daher keinen Wert. Sollte Garmin ihn künftig als zugänglichen Datentyp/Metadatum bereitstellen, kann er ergänzt werden.
