# ADR-0004: Hebrew as the sole UI language, RTL forced at app root

## Status
Superseded in part (2026-11)

> **Amendment (2026-11).** The app is going to the public Play Store with English as the
> main language. Interface text lives in string resources (`values` and `values-iw` in
> Hebrew, `values-en` in English), and RTL is no longer forced: `GymTrackerTheme` leaves
> the layout direction to Android, which sets it from the app's locale (right to left in
> Hebrew, left to right in English; `supportsRtl` stays on). A few rows that should look
> the same in both languages pin themselves left to right: History's month header
> ("← previous, month, next →") and the run target stepper ("− value +"), next to the
> charts that already did. Exercise names in `TrainingPlanData` are still Hebrew until
> the exercise library (ADR-0006).

> **Amendment (2026-10).** Hebrew stays the default, but an English translation now
> exists (`values-iw` and `values-en`), chosen in Settings. Hebrew screens wrap numeric
> ranges in Unicode isolates (U+2066…U+2069) so they don't read backwards. Canvas
> drawings such as charts and the week strip mirror themselves for right-to-left.

> **Amendment (2026-11).** English is now the main language (the app is going public on
> Play): the default `values/` strings are English and Hebrew lives in `values-iw/`
> (`values-en/` is gone). A fresh install is no longer switched to Hebrew; it follows the
> phone, so a Hebrew phone gets Hebrew and any other language gets English. An install that
> already has a language (every one so far, as the old code set Hebrew on first launch)
> keeps it. The run voice follows the same choice.

## Context
This is a personal app for a single Hebrew-speaking user. All training content, exercise names, and UI text are in Hebrew. Options considered:
- **Full localisation (strings.xml + locale detection)** — supports multiple languages but adds overhead and this app will never need it
- **Hebrew only, RTL forced via CompositionLocalProvider** — single strings.xml in Hebrew, layout direction set programmatically at the theme root
- **Device locale** — depends on the device being set to Hebrew; breaks if the user tests on an English device

## Decision
All user-facing strings are in Hebrew in the single `values/strings.xml`. RTL layout direction is forced unconditionally via `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` inside `GymTrackerTheme`, regardless of device locale. Hebrew exercise names are hardcoded in `TrainingPlanData`.

## Consequences
- **Good:** layout is always correct regardless of device language setting
- **Good:** no i18n overhead, no missing-translation bugs
- **Bad:** if the app is ever shared with a non-Hebrew speaker, full localisation work is needed
- **Bad:** forcing RTL in Compose overrides system locale for all composables — third-party components that rely on system LayoutDirection may render unexpectedly (acceptable at this scale)
- Android's `android:supportsRtl="true"` in the manifest is required and present
