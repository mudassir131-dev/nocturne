# Security 17: FileProvider Content URI Permissions for Playlist Exports

## Permissions
- Playlist export files shared strictly through Android `FileProvider` with temporary read grants.
- Direct external storage path sharing (`file://`) prohibited to adhere to scoped storage.
