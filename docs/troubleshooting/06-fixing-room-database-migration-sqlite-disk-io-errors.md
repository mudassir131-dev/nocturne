# Troubleshooting Guide 06: Resolving Room Database Migration and SQLite Disk I/O Errors

## Symptom Overview
App crashes upon launch following an update with `IllegalStateException: Room cannot verify the data integrity` or `SQLiteDiskIOException: disk I/O error`.

## Root Cause
- Missing `Migration` path from an older schema version (e.g., version 7 to version 8).
- Database file corruption resulting from abrupt device shutdown during SQLite WAL checkpointing.

## Diagnostic Procedure
1. Examine stacktrace for:
   ```
   java.lang.IllegalStateException: A migration from X to Y was required but not found.
   ```
2. Verify table hash in Room metadata table `room_master_table`.

## Recovery and Fixes
- **Add Explicit Migration**:
  Always declare non-destructive explicit migrations:
  ```kotlin
  val MIGRATION_7_8 = object : Migration(7, 8) {
      override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE Song ADD COLUMN bpm REAL DEFAULT 0.0 NOT NULL")
      }
  }
  ```
- **Fallback Policy**:
  In extreme cases of disk corruption, ensure `fallbackToDestructiveMigrationOnDowngrade()` is guarded by automated JSON backup snapshots.\n