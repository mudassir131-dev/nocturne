# Storage 10: SQLite File Locking & Concurrency Isolation Between Services

## Isolation
- Thread-safe repository patterns mediate concurrent read/write operations between UI and `PlaybackService`.
- Kotlin `Mutex` and Room transactional blocks avoid database locked exceptions.
