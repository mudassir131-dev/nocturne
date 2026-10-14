# Subsystem API Specification 08: Spotify Canvas Protobuf Entity and CDN Stream URL Contract

## Schema
Represents video canvas background streaming:
- `canvas_url`: Fully qualified MP4 video URL on Akamai CDN.
- `track_uri`: Canonical Spotify URI `spotify:track:xyz`.
- `artist_name`: Display name.

## Invariant
- Video streams are restricted to 720p or lower to save bandwidth.
- Audio tracks in Canvas mp4 containers are stripped or muted during playback.\n