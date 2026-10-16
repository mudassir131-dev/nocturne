# Runbook 08: AndroidX Media3 ExoPlayer Upgrade & API Migration

## Objective
Guidance for bumping `androidx.media3` libraries while ensuring zero regressions in custom media sources, decoders, and playback notifications.

## Checkpoints
- Audit deprecations in `MediaSession.Callback`, `Player.Listener`, and `ForwardingPlayer`.
- Verify custom `DataSource.Factory` chunk pipeline maintains thread safety.
- Test Bluetooth AVRCP metadata sync on physical automotive/headset units.
