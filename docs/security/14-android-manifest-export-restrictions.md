# Security 14: Android Manifest Component Export Gates & Permissions

## Sandboxing
- All Activities, Services, and Receivers marked `android:exported="false"` by default.
- Only the media playback notification receiver is explicitly exposed with permission gates.
