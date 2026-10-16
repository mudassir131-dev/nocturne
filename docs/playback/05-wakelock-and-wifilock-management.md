# Playback 05: Power Manager Partial WakeLock & High-Perf WiFi Lock Rules

## Energy Management
- Acquires `PARTIAL_WAKE_LOCK` and `WIFI_MODE_FULL_HIGH_PERF` exclusively during active playback.
- Released immediately within 500ms of playback pausing to prevent standby battery drain.
