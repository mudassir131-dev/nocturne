# Subsystem API Specification 12: Playlist Importer CSV Streaming Parser and Normalizer Contract

## Supported Headers
Recognizes standard CSV exports from:
- Spotify: `Track Name,Artist Name(s),Album,Duration (ms)`
- Apple Music: `Title,Artist,Album,Time`
- YouTube Music: `Title,Channel,Duration`

## Normalization Pipeline
1. Sanitize unicode characters and accents.
2. Strip metadata suffixes (`feat.`, `ft.`, `Remastered`).
3. Stream lines through buffer with memory consumption $\le 2$ MB.\n