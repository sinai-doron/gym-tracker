# ADR-0001: Local-only storage, no backend, no authentication

## Status
Accepted, amended 2026-10; superseded in part 2026-11 (optional sign-in and sync)

> **Amendment (2026-10).** Storage is still local-only, but the app is no longer fully
> offline. It reads map tiles (OpenFreeMap), weather (Open-Meteo) and street names for
> routes (Nominatim), and exchanges runs, steps and heart rate with Health Connect on the
> phone. No workout data is sent to a server. A manual backup to a zip (Settings → Backup)
> covers the "device is wiped" risk below.

> **Amendment (2026-11).** The app is public on Google Play, so it is no longer for a single
> user, and "no backend, no authentication" no longer holds in full. The phone's Room database
> is still where the data lives, and the app still works with no account. What changed:
>
> - **Optional sign-in and sync.** A user can sign in with Google (Credential Manager) and
>   sync to the owner's server, doron-backend at `apps.neonsheep.dev`, through a
>   `sync_changes` log filled by database triggers and sent by a WorkManager job. Nothing
>   syncs until the user agrees on a consent screen; run routes and the music log are
>   separate choices. The first account to sign in owns the phone's data and any other
>   account is refused, so one person's workouts never land in another's account.
> - **Switched off for the public release.** Sign-in sits behind a switch (`SignInFeature`):
>   off, the welcome screens skip the account page and Settings has no Account section. It is
>   on only on a phone already signed in, or when switched on by hand (seven taps on the
>   version line in Settings). Turning it on for everyone needs Play's app-signing key on the
>   OAuth client, per-user quotas on the server and an updated Data safety form.
> - **Health Connect data never syncs.** Daily steps and heart rate are read from Health
>   Connect for display and stay on the phone; so do a run's steps over time (`run_steps`,
>   database version 18).
>
> The consequences below about multi-device access and a wiped phone now apply only to users
> who don't sign in; for them the backup zip remains the answer.

## Context
This is a personal-use app for a single user. No sharing, no sync across devices, and no account management are required. Options considered:
- **Local Room database** — all data on-device, no network dependency
- **Cloud backend (Firebase / Supabase)** — enables multi-device sync and remote backup
- **File-based storage** — simpler than Room but no relational querying

## Decision
Store all data locally using Android Room. There is no backend, no authentication, and no network layer. The app works fully offline and requires no account.

## Consequences
- **Good:** zero latency, no auth complexity, works without internet, no server cost
- **Good:** no sensitive data leaves the device
- **Bad:** data is lost if the device is wiped and no backup is made
- **Bad:** no multi-device access — if the user switches phones they lose history (acceptable for a personal tool)
- If sync is ever needed it will require a meaningful architecture change; this decision should be revisited at that point
