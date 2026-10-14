# Troubleshooting Guide 15: Fixing Audio Noisy Broadcast Receiver Unregister Crashes

## Symptom Overview
Unplugging wired headphones or disconnecting Bluetooth earphones does not pause music playback immediately, blasting audio through the loudspeaker, or app crashes with `IllegalArgumentException: Receiver not registered`.

## Root Cause
`AudioManager.ACTION_AUDIO_BECOMING_NOISY` receiver registered or unregistered outside synchronized lifecycle callbacks of `MediaSessionService`.

## Resolution Pattern
- **Safe Lifecycle Registration**:
  ```kotlin
  override fun onPlay() {
      if (!isNoisyReceiverRegistered) {
          registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
          isNoisyReceiverRegistered = true
      }
  }
  
  override fun onPause() {
      if (isNoisyReceiverRegistered) {
          unregisterReceiver(noisyReceiver)
          isNoisyReceiverRegistered = false
      }
  }
  ```
- Handle `onReceive` by immediately calling `player.pause()`.\n