# Subsystem API Specification 11: Last.fm Track Scrobble and Love Status API Service Contract

## Method Calls
- `track.updateNowPlaying`: Dispatched when a track begins playing.
- `track.scrobble`: Dispatched when playback threshold (50% or 4 min) is met.
- `track.love` / `track.unlove`: Dispatched upon user favorite toggle.

## Signature Invariant
All requests require HMAC-MD5 signing with Nocturne API Secret.\n