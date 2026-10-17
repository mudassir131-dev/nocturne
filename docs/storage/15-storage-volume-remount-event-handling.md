# Storage 15: Android External SD Card Remount Detection and Library Rescanning

## SD Card Detection
- BroadcastReceiver listens for ACTION_MEDIA_MOUNTED and ACTION_MEDIA_UNMOUNTED.
- Dynamically pauses background workers and refreshes directory paths without restarting app.

