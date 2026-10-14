# Troubleshooting Guide 03: Bypassing Aggressive OEM Background Battery Killers

## Symptom Overview
Nocturne background music playback terminates unpredictably after 5 to 15 minutes of screen-off time on devices from Xiaomi (MIUI/HyperOS), Huawei (EMUI), OnePlus (OxygenOS), and Samsung (One UI).

## Technical Cause
OEM task managers kill foreground services that do not hold a permanent `PARTIAL_WAKE_LOCK` or whose notification channels lack ongoing persistent priority flags, ignoring standard Android Open Source Project (AOSP) background execution limits.

## Resolution Steps for Developers & Power Users
1. **Acquire Partial WakeLock**:
   Ensure `PlaybackService` acquires `PowerManager.PARTIAL_WAKE_LOCK` with a descriptive tag `Nocturne:PlaybackWakeLock` during active audio playback and releases it immediately upon pause:
   ```kotlin
   wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Nocturne:PlaybackWakeLock").apply {
       setReferenceCounted(false)
       acquire(10 * 60 * 1000L) // Safety timeout
   }
   ```
2. **Foreground Notification Optimization**:
   Ensure notification uses `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK` on Android 14+ (API 34).
3. **User Prompt for Battery Exemption**:
   Prompt the user to whitelist Nocturne via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

## Verification Checklist
- Run screen-off background playback test for 120 consecutive minutes.
- Verify zero unexpected `onDestroy()` lifecycle calls in `PlaybackService`.\n