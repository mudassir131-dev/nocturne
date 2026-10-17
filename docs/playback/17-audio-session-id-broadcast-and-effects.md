# Playback 17: Audio Session ID Lifecycle & Visualizer Effect Attach

## Audio Session Management
- Exposes stable audioSessionId across ExoPlayer rebuilds for external and system visualizer attachments.
- Broadcasts android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION upon pipeline initialization.

