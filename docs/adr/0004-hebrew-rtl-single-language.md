# ADR-0004: Hebrew as the sole UI language, RTL forced at app root

## Status
Accepted, amended 2026-10

> **Amendment (2026-10).** Hebrew stays the default, but an English translation now
> exists (`values-iw` and `values-en`), chosen in Settings. Hebrew screens wrap numeric
> ranges in Unicode isolates (U+2066…U+2069) so they don't read backwards. Canvas
> drawings such as charts and the week strip mirror themselves for right-to-left.

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
