# Security & Privacy Policy 08: Android Manifest Component Export Rules and Permission Gates

## Manifest Security
- All `Activity`, `Service`, and `Receiver` components not intended for external invocation must explicitly set `android:exported="false"`.
- `PlaybackService` must be protected by `android.permission.FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.\n