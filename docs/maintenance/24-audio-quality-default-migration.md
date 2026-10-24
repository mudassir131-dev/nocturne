# 24 - Audio Quality Default Migration

Documents the preference migration that retunes the default listening path
from the legacy LOSSLESS stamp to YouTube Opus.

## Why a migration, not just a default change

Every pre-existing install had `LOSSLESS` written into DataStore by the
earlier sources migration (`audioQualityMigratedToSourcesV1`). Changing only
the in-code fallback default therefore changed nothing for devices the app
was already running on — the stored value kept winning.

## Migration chain

Registered in `DataStore.kt`, applied in order:

1. `OnboardingMigration`
2. `PlayerArchitectureMigration`
3. `AudioQualityMigration` (V1) — legacy bitrate presets → source modes
   (`SAAVN` explicit choice and the removed `LOW` tier are preserved; all
   other legacy values now land on `OPUS` instead of `LOSSLESS`)
4. `AudioQualityOpusDefaultMigration` (V2, one-shot) — flips a stored
   `LOSSLESS` value to `OPUS` exactly once, guarded by the
   `audioQualityDefaultOpusV2` flag

## Contract

- Stored `SAAVN` (an explicit choice) is **never** touched — users who opted
  into the catalog keep it.
- An unset preference stays unset; the in-code default (`OPUS`) applies at
  read time for fresh installs.
- The migration flips once and never re-runs; mind-the-gap runs of the
  DataStore make it idempotent and crash-safe.

## Covered by

`AudioQualityOpusDefaultMigrationTest` — the flip, the preserved explicit
choice, the unset no-op, and the once-only guarantee.
