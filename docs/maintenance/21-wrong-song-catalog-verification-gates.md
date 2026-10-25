# 21 - Wrong-Song Root Cause & Catalog Verification Gates

Documents the playback trust chain hardening added after the reported wrong-song
cases: playing "12 Saal" (Bilal Saeed) and "Brown Rang" (Yo Yo Honey Singh)
resolved into a different recording's audio.

## Root cause

- `OnlineLosslessAudioProvider` (active when Audio Quality = LOSSLESS or SAAVN,
  and Data Saver is off) accepted the **first** Saavn search result that came
  within a ±12 s duration window. It never verified the title or the performer.
- Saavn's catalog is full of same-name rows by other performers (covers,
  re-records, remixes, preview cuts). The first row therefore frequently was a
  different artist wearing the searched song's name.

## Verification contract (final)

| Gate | Rule |
|---|---|
| Title floor | `TrackMatcher.titleScore(*)` must reach `0.35` after the variant penalty |
| Variant reject | `hasUnwantedVariant` (remix / live / cover / slowed / sped / …) multiplies the title score by `0.3` |
| Duration hard-reject | A known duration further off than `15 s` is a different recording |
| Artist verification | `TrackMatcher.artistVerified`: word-level name containment in either direction, or similarity `≥ 0.70` for spelling variants — fuzzy similarity alone may never authenticate a performer |
| Unverifiable artist | When the performer cannot be verified on either side (no artist fields on the row, or a title-only retry), the duration must fingerprint the master: delta `≤ 5 s` |
| Confidence | Weighted `0.45 title / 0.25 artist / 0.30 duration`, threshold `0.55`; the **best** surviving candidate wins, never the first result |

## Why fuzzy-only artist matching was banned

`levenshteinRatio("bilal saeed", "sarmad qadeer")` measures `0.39` — above the
naive `0.35` floor reused from the title gate, yet the two names are different
people. Word-level containment or near-letter-for-letter agreement (for
"Arman"/"Armaan Malik" style billing variants) is the only acceptable proof.

## Regression coverage

- `SaavnWrongSongRegressionTest` — recorded Saavn response shapes for the two
  reported songs: wrong-artist rows, remix variants, slowed+reverb variants,
  preview-cut durations, and the genuine masters. Lookalikes-only result sets
  must return `null` (playback falls back to the correct YouTube stream).
- `TrackMatcherTest` — the artist verification truth table, including the
  Bilal Saeed / Sarmad Qadeer trap and real billing variants.

## Playback-chain audit result

The streaming pipeline re-resolves exclusively by the playing track's
`videoId`; queue, preload and recovery paths are `mediaId`-keyed. The catalog
providers were the only per-song substitution points, and both (Saavn and
self-hosted) now enforce the same gates.

## Raw-max policy note

`25-raw-max-bitrate-per-song-playback-policy.md` exposes the catalog to every
quality mode and ranks the YouTube fallback candidates by available bitrate.
It does not touch the gates above: an unverifiable candidate stays rejected,
and a catalog 320 kbps AAC is served only for the exact verified master.
