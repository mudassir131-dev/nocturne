# ADR-021: Raw-Max Bitrate Per-Song Source Selection

## Status
Accepted

## Context
Every quality mode previously resolved from YouTube first, and YouTube's free
audio ceiling is `itag 251` Opus at ~160-168 kbps for typical uploads, so every
song reported the same bitrate regardless of what the verified catalog could
serve. Live probes additionally surfaced that some CDN deployments mishandle
HTTP HEAD, which failed the stream preflight for valid high-bitrate candidates
and silently degraded playback to the fallback stream.

## Decision
Run the verified per-song catalog chain (self-hosted lossless → local
FLAC/ALAC → Saavn 320 kbps) for all quality modes while Data Saver is off, so
the rawest verified source wins for each exact track. On the YouTube fallback,
rank the available audio renditions by bitrate first and use the mode's codec
preference only as a tie-breaker. Probe catalog URLs with a `Range: bytes=0-0`
GET instead of a HEAD before serving them.

## Consequences
Verified catalog matches stream at the catalog's own bitrate (lossless stays
lossless, `_96/_160` catalog tokens upgrade to `_320`), songs without a
verified match keep playing the exact same `videoId` through YouTube, and the
per-song verification gates from `docs/maintenance/21` are unchanged, so
exactness is never traded for bitrate.
