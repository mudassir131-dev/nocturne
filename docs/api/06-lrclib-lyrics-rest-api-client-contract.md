# Subsystem API Specification 06: LRCLib Synced and Plain Text Lyrics Query API Contract

## Endpoint
`GET https://lrclib.net/api/get`

## Query Parameters
- `track_name`: String (required)
- `artist_name`: String (required)
- `album_name`: String (optional)
- `duration`: Int (seconds, optional)

## Response Contract
- `syncedLyrics`: Standard LRC string `[mm:ss.xx] line content`.
- `plainLyrics`: Unsynchronized lyric text.
- Fallback: If 404 is returned, fail gracefully to Kugou or secondary provider.\n