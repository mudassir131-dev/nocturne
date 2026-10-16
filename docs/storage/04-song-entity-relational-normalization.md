# Storage 04: Room Database Relational Normalization & Foreign Keys

## Schema Design
- `songs`, `playlists`, `playlist_song_map`, and `history` tables normalized with foreign key cascading.
- Indices created on `videoId`, `artistId`, and `timestamp` for sub-millisecond query response times.
