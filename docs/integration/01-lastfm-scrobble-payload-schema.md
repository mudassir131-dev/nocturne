# Integration 01: Last.fm Scrobble POST Payload Schema

## Specifications
- Dispatches `track.updateNowPlaying` upon playback start.
- Dispatches `track.scrobble` after track completes 50% or 4 minutes of playback.
- Offline scrobble queue persists records until network connectivity is re-established.
