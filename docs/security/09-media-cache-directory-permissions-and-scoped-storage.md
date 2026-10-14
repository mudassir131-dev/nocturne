# Security & Privacy Policy 09: App-Specific Internal Cache Isolation and Scoped Storage Rules

## Rules
- Audio chunk and lyrics caches must reside in `context.cacheDir` or `context.noBackupFilesDir`.
- Storage access must adhere strictly to Android Scoped Storage rules without requiring `MANAGE_EXTERNAL_STORAGE` broad permissions.\n