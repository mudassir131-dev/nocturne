# Storage 02: SQLite WAL Checkpointing & PRAGMA Tuning for Room

## Configuration
- `PRAGMA journal_mode = WAL;`
- `PRAGMA synchronous = NORMAL;`
- Reduces disk write amplification while ensuring full crash resilience and ACID compliance.
