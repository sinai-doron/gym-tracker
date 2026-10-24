# ADR-0001: Local-only storage, no backend, no authentication

## Status
Accepted

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
