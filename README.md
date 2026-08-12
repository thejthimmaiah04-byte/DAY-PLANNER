# ARC — Offline Day Planner

A fully-offline Android day planner built with Kotlin + Jetpack Compose.

## Features
- **Tasks** with priority (Urgent / High / Medium / Low) and category
  (Work / 2-Year Vision / Skill Building / Personal), notes, and time estimates.
- **Auto day plan** — every day, pending and overdue tasks are ranked by priority
  and laid into time blocks (8:00–22:00) so you always know what to do next.
- **Reminders** — exact alarms with notifications, restored after reboot.
- **Calendar** — month view with priority dots, tap a day to see and add tasks.
- **Progress analytics** — 7-day weighted efficiency score, day streak,
  completions chart, focus hours, and per-category balance.
- **100% offline** — Room (SQLite) storage, no network permission at all.

## Design
- Palette: deep ink `#0B0C10` (60%), warm ivory text `#F2EEE5` (30%),
  champagne gold `#D4B476` accent (10%) — classic 60-30-10 rule.
- Typography: Fraunces (display serif) + Inter (UI), bundled offline.

## Build
```
gradlew.bat assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

Install on a connected phone (USB debugging on):
```
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and the Android SDK (compileSdk 35).
