# Małe Kroki — personal streaks & growth tracker

A native Android app (Kotlin, Jetpack Compose, Material 3) for building sustainable habits through tiny daily actions. Five minutes counts. Missing a day is just a day. The UI is entirely in Polish; code and docs are in English.

Primary target: Samsung Galaxy S24 (Android 14/15, One UI 6/7). Works on any phone with Android 8.0+.

## Features

| Area | What it does |
| --- | --- |
| **Dzisiaj** | Date, greeting, today's habits with one-tap completion, current and best streak, this week overview, recent learning, daily reflection, quick "Sesja nauki" button, small celebration (haptic + animated check + snackbar with undo). |
| **Nawyki** | Create, edit, archive, restore, delete habits. Emoji, category, description, completion criteria (minutes or a single action), optional "one understood concept" rule, optional target, planned weekdays, streak on/off, learning on/off. Four editable starter habits on first launch. |
| **Kalendarz** | Month grid per habit or all habits (heatmap), legend, distinct states for completed / no entry / leave / scheduled leave / today, 7- and 30-day activity, total learning time and sessions. Tap a day to fix past entries. |
| **Dziennik** | Searchable learning journal: topic, minutes, what was learned, questions to revisit, notes, practical exercise. Summary: total time, sessions, recent topics, 8-week chart. Manual entry or simple timer. |
| **Ustawienia** | Theme (system/light/dark), Material You colours, optional daily reminder (off by default), JSON export/import with validation and merge/replace confirmation, delete all data. |
| **Nieobecności** | Leave periods (vacation, sick, holidays, personal break, custom) for all or selected habits; protected days preserve streaks without counting as completed. |

## Architecture

```
app/src/main/java/com/malekroki/app/
├── domain/            Pure Kotlin, no Android: models, StreakCalculator, LeaveCalendar/LeaveRules,
│   │                  CompletionRules, Statistics, PolishText, StarterHabits
│   └── backup/        JSON DTOs, BackupValidator, BackupMapper, BackupMerger
├── data/              Room database (db/), AppRepository, SettingsRepository (DataStore),
│                      DateProvider, BackupJson
├── notifications/     ReminderScheduler + receivers (one inexact daily alarm)
└── ui/                Compose screens + ViewModels per feature, theme, navigation
```

* **Layers:** UI (Compose + ViewModel + StateFlow) → `AppRepository` → Room/DataStore. All business rules live in `domain/` and are unit-tested without Android.
* **DI:** a tiny manual `AppContainer` in `MaleKrokiApp`; no framework needed for an app this size.
* **Reactive data:** DAOs expose `Flow`s; screens derive streaks and statistics from the full (small) data set, so every change (completion, session, leave, edit) is reflected everywhere immediately and consistently.
* **Transactions:** session save/delete, leave save, import and delete-all each run in one Room transaction.

### Date handling strategy

* Every completion, session and leave is stored as a **`LocalDate` (epoch day)** computed in the device's current time zone at the moment of the action. No timestamps are compared for calendar logic.
* "Today" comes from `DateProvider`, which reads `Clock.systemDefaultZone()` on every call, so time-zone changes and midnight are picked up while the app is open.
* Material date pickers report UTC midnight; `DateMath.fromPickerMillis` converts with UTC, so a picked date never shifts by a day.
* DST, month/year boundaries and leap years are handled by `java.time` calendar arithmetic and covered by tests.

### Streak rules

For each habit and day the engine (`StreakCalculator.dayStatus`) assigns one status:

| Status | Effect on streak |
| --- | --- |
| COMPLETED | +1 |
| MISSED (planned past day, no completion, no leave) | ends the current run |
| PROTECTED (leave, not done) | skipped — neither breaks nor extends |
| REST (not a planned weekday) | skipped |
| PENDING (today, not done yet) | skipped — today never counts as missed |
| BEFORE_START | stops the walk |

Current streak walks back from today; longest streak scans forward with the same rules, so they can never disagree. Completions are unique per `(habitId, date)` at the database level (composite primary key), which makes duplicates impossible. Removing or editing a leave or completion simply changes the inputs; nothing is cached, so streaks are always recalculated from real history.

### Learning sessions → completion

Minutes of all sessions for a habit on the same day are summed. If the sum reaches the habit's minimum (or a session is marked "one concept understood" and the habit allows it), a completion with source `SESSION` is created. Editing or deleting sessions re-evaluates that day; session-derived completions are removed if the day no longer qualifies, manual completions are never removed automatically.

### Leave (Nieobecności)

`LeaveCalendar` answers "is habit X protected on day D" as a set membership question, so overlapping leaves never double-count. Leaves apply to all habits (including ones created later) or to selected habits. Leave never creates completions. Exact duplicates are rejected; overlaps are allowed and shown as an informational note.

### Privacy

No internet permission, no analytics, no ads, no account. Android cloud backup and device-transfer extraction are disabled (`allowBackup=false`, `data_extraction_rules.xml`); use the JSON export instead.

### Minimum SDK

`minSdk 26` (Android 8.0): `java.time` is available natively without desugaring, notification channels and adaptive icons are standard, and it still covers the vast majority of devices in use. `targetSdk/compileSdk 35`.

## Build

Requirements: Android Studio (Ladybug 2024.2 or newer) with the bundled JDK 17+, Android SDK 35. Internet access is needed on the first build to download Gradle and dependencies.

```bash
./gradlew testDebugUnitTest     # JVM unit tests (domain logic + JSON round trip)
./gradlew assembleDebug         # → app/build/outputs/apk/debug/app-debug.apk
./gradlew connectedDebugAndroidTest   # Room/repository tests, needs a device or emulator
```

In Android Studio: **File → Open** the project folder, wait for Gradle sync, then **Build → Build App Bundle(s) / APK(s) → Build APK(s)**, or press Run with the phone connected.

### Build in the cloud (no Android Studio)

Push the project to a GitHub repository. The workflow in `.github/workflows/android.yml` runs the unit tests and builds the debug APK; download it from the run's **Artifacts** section (`male-kroki-apk`).

## Install on the Galaxy S24

1. Copy `app-debug.apk` to the phone (USB, Google Drive, e-mail to yourself…).
2. Open it from **My Files**. Android asks to allow installing unknown apps for that source (e.g. My Files or Chrome) — allow it.
3. If Samsung **Auto Blocker** is on (Settings → Security and privacy → Auto Blocker), turn it off temporarily, install, then turn it back on.
4. With USB debugging enabled you can instead run `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

The debug build installs as "Małe Kroki" with package `com.malekroki.app.debug`, so it can live alongside a future release build.

## Tests

* `app/src/test` — JVM unit tests: streak engine (month/year/leap-year/DST boundaries, duplicates, removal, rest days), leave protection (selective scope, overlaps, edits/deletion, year boundary, today/future), completion rules (5-minute minimum, summing, concept rule, sync), statistics, backup validation/merge/round trip, Polish wording (including a check for shaming words), JSON text round trip.
* `app/src/androidTest` — Room repository tests: unique completions, session ↔ completion sync, cascades, history preserved on edit, leave persistence, export → wipe → import round trip.

## Extending

Strings are inline Polish literals in the Compose code for speed of iteration; move them to `strings.xml` if you ever want another language. The Room schema is exported to `app/schemas/`; add a `Migration` when changing entities.
