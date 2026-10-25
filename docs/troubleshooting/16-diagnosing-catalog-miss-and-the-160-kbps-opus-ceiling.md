# Troubleshooting Guide 16: Diagnosing Catalog Misses and the ~160 kbps Opus Ceiling

## Symptom Overview
A song plays back reported as `OPUS 160-168 KBPS` even though the user selected OPUS or LOSSLESS, and users report it as "the app is stuck at 160 kbps". The badge identical for every track is the signature that the per-song catalog did not match.

## Root Cause Pattern
- YouTube's free audio ceiling for typical uploads is `itag 251` Opus at ~160-168 kbps; no free client can pull more for those songs.
- Streams only move above that when the per-song verified catalog matches (`_320.mp4` Saavn master, self-hosted lossless, or local FLAC/ALAC). See `docs/maintenance/25-raw-max-bitrate-per-song-playback-policy.md`.

## Diagnostic Procedure
1. Reproduce with the quality log: search `LOSSLESS_PIPELINE` / `OnlineLosslessProvider` in logcat.
   ```bash
   adb logcat -s OnlineLosslessProvider:D LosslessStreamResolver:D
   ```
2. Check whether the Saavn catalog was even consulted (`Searching online lossless stream for: ...`).
3. If consulted, read the candidate verdicts (`Candidate '...' -> title=..., confidence=..., REJECTED: ...`) to see which gate rejected the best row (title floor, artist verification, duration fingerprint, or confidence threshold).
4. Confirm the resolved tier: `_320.mp4` (catalog master) vs `itag 251` (YouTube fallback).

## Remediation Steps
- **Ceiling is upstream**: when nothing matches and the video carries only standard renditions, `OPUS ~160 kbps` is already YouTube's maximum — not a regression in the fetch pipeline.
- **Data Saver gates the catalog**: the catalog is intentionally skipped under Data Saver; check Settings -> Player -> Data Saver before treating a miss as a bug.
- **Metadata fixes find masters**: correcting a track's title/artist metadata in the local library changes the search query and can turn a catalog miss into a match.
- **Pre-2.22.39 head-probe failures**: CDN deployments that rejected HTTP HEAD failed the preflight for valid high-bitrate candidates and silently degraded whole sessions to the fallback; `LosslessStreamResolver.validateRemoteUrl` now probes with a `Range: bytes=0-0` GET.

## Invariant Verification
- A lookalike catalog row must never win; lookalikes-only result sets must return `null` (`SaavnWrongSongRegressionTest`).
- A verified catalog match always outranks a YouTube rendition at any bitrate.
- Unmatched tracks still play the exact same `videoId` through the YouTube pipeline.
