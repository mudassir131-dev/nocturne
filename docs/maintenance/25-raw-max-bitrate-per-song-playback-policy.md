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
