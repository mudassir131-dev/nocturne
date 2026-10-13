# FAQ: How are Song entities linked to cached audio files in Room?

## Question
How are Song entities linked to cached audio files in Room?

## Answer
Room stores track metadata in the SongEntity table, keyed by unique YouTube video ID. ExoPlayer SimpleCache indexes audio chunks on disk using the video stream URL cache key. When checking offline status, the repository queries SimpleCache.isCached(key) to determine if a song is ready for offline play.
