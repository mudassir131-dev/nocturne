# 25 - Raw-Max Bitrate Per-Song Playback Policy

Supersedes the *no per-song catalog mixing* clause of
`22-youtube-opus-default-playback-policy.md`. Documents the retune that makes
every quality mode stream the rawest source available for each exact track
instead of pinning all songs to YouTube's Opus ceiling.

## Why the policy changed

Verification against the live sources showed two facts:

1. **YouTube's own ceiling is `itag 251` Opus at ~160 kbps.** `yt-dlp` format
   dumps list `250/249 (opus 55-72k)`, `140 (mp4a 129k)` and `251 (opus ~140-168k)`
   as the best audio-only renditions for typical songs. No free client behaves
   differently; a per-song `280/320 kbps` Opus rendition only exists when the
   specific upload actually carries it. So the previous "Opus end to end"
   default structurally reported `OPUS 160-168 kbps` for every song simply
   because YouTube serves nothing higher there — it was not a fetch bug.
2. **The verified catalog streams are strictly rawer where they match.**
   Saavn's encrypted media URLs decrypt to `_320.mp4` AAC transports which
   respond `200` (full) / `206` (Range) with `content-type: audio/mp4`.
   The verification gates (title floor, artist verification, duration
   fingerprint, confidence threshold) from
   `21-wrong-song-catalog-verification-gates.md` are unchanged.

## Policy

- **Catalog first, for every mode.** `playerResponseForPlaybackOnce` runs the
  `LosslessStreamResolver` chain (self-hosted Navidrome/Subsonic → local
  FLAC/ALAC → Saavn 320 kbps catalog) for **all** quality modes whenever
  Data Saver is off. A verified match is served as-is — lossless stays
  lossless, a catalog 320 kbps AAC is served raw (never transcoded, never
  resampled); the suffix upgrade `_96/_160 → _320` remains the maximum the
  catalog publishes.
- **No mode is a YouTube-only lock anymore.** The `Opus` label keeps meaning
  "default listening mode", not "only ever YouTube". Under any mode a catalog
  match plays at the catalog's bitrate; unmatched songs still play the exact
  same `videoId` through YouTube.
- **Highest-bitrate wins on the YouTube fallback.** Both stream paths rank
  candidates by available bitrate first with the mode's codec preference as a
  tie-breaker (SAAVN keeps its AAC-first identity). A raw higher-bitrate
  rendition (e.g. `itag 141` AAC 256 kbps, or a high-res opus format) is never
  passed over for a smaller fixed-160 kbps opus on the modes that allow it.
- **Data Saver is untouched** (64 kbps target, catalog skipped, low-bitrate
  preference).
- **Exactness is never traded for bitrate**: a catalog candidate that cannot
  be verified against the track's title/artist/duration is rejected and never
  becomes "a rawer but different song".

## What each mode plays now

| Situation | Stream | Badge |
|---|---|---|
| Verified catalog match (any mode) | Saavn 320 kbps AAC, self-hosted lossless or local FLAC/ALAC | `SAAVN 320 KBPS` / `Lossless` |
| No catalog match, YouTube carries only standard renditions | itag 251 Opus ~160-168 kbps (`YouTube` ceiling) | `OPUS 160-168 KBPS` |
| No catalog match, video carries a higher rendition | that rendition (e.g. AAC 256 kbps) | `AAC 256 KBPS` |

`OPUS ~160 kbps` for a catalog-missed song is therefore the maximum that
exists upstream, not a regression.

## Preflight hardening

`LosslessStreamResolver.validateRemoteUrl` now issues
`GET` with `Range: bytes=0-0` instead of `HEAD`. HEAD mishandling on some
CDN deployments previously failed the reachability check for valid candidates
and silently degraded whole sessions to the fallback stream.

## Covered by

- Live-source verification: Saavn catalog `_320.mp4` probes, yt-dlp format
  listing for reference tracks.
- `SaavnWrongSongRegressionTest` / `OnlineLosslessAudioProviderTest` — the
  exact-track gates still hold.
- `AudioQualityOpusDefaultMigrationTest` — default-stamp mechanics unchanged.
- ADR-021 (`adr-021-raw-max-bitrate-per-song-source-selection.md`) records
  the decision as an architectural standard.
