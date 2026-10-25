# 22 - YouTube Opus Default Playback Policy

Documents the retuned default listening path and why per-song source mixing
was retired.

> **Partly superseded by `25-raw-max-bitrate-per-song-playback-policy.md`.**
> The "no per-song catalog mixing" clause was reversed after verification
> against the live sources showed YouTube's Opus ceiling sits at ~160 kbps for
> typical songs while verified catalog matches stream at 320 kbps / lossless.
> Every mode now runs the per-song verified catalog first and the YouTube
> fallback picks the highest available bitrate. The opus-first *default stamp*
> mechanics below still apply to fresh installs and the preference migration.

## Policy

- **Default Audio Quality = OPUS**, end to end. Every song is served from
  YouTube's best Opus stream for that exact `videoId` — the track's real
  metadata is the fetch key, never a re-search.
- **Highest bitrate wins inside the codec**: candidate ranking is
  Opus-first, then bitrate descending, up to a `320 kbps` target — so the
  strongest Opus rendition YouTube meets (including ~300 kbps peaks on
  high-res-opus endpoints) is selected before anything downgrades to AAC.
  AAC exists only as a last-resort fallback when no Opus candidate can be
  extracted (NewPipe first, then InnerTube clients, Opus-capable clients
  tried first).
- **No per-song catalog mixing.** The old default (LOSSLESS) resolved each
  song from the Saavn AAC 320 kbps catalog when a verified row existed and
  otherwise fell back to YouTube Opus — playback heard a different codec and
  a different catalog match from song to song. Consistency beats peak
  bitrate per song; the catalog source is now strictly opt-in.

## Where it applies

| Consumer | Default |
|---|---|
| `MusicService` streaming resolution | OPUS |
| `DownloadUtil` offline resolution | OPUS |
| Player Settings audio-quality selector | OPUS listed first |
| Apple-style audio output sheet | OPUS listed first, selected by default |

## Opt-in modes that remain

- **Hi-Res Lossless** — Saavn/self-hosted catalog lookups with the same
  verification gates as documented in `21-wrong-song-catalog-verification-gates.md`.
- **Saavn** — AAC-320 kbps catalog lookups, same gates.

Both modes still fall back to the standard YouTube stream when no verified
match exists: they widen the pool, never the acceptance rules.

## Self-hosted server parity

Self-hosted (Navidrome / Subsonic) catalog rows enforce the identical gates —
lossless format inspection, unwanted-variant rejection, verified-artist rule,
and the ≤ 5 s master fingerprint when the performer cannot be verified.
