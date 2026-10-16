# Testing 08: Automated Memory Leak Detection & Activity Retention Checks

## Target Vectors
- Verifies `MainActivity`, Compose composables, and `PlaybackService` disconnect all listener refs.
- Ensures native JNI handles are deallocated promptly upon player release.
