# UI Spec 07: Tactile Haptic Feedback Curves for Playback Controls

## Haptic Definitions
- **Play/Pause Toggle**: `HapticFeedbackType.LongPress` or `VibrationEffect.createPredefined(EFFECT_CLICK)`.
- **Seek Scrubbing**: Granular micro-ticks (`EFFECT_TICK`) emitted at 1-second scrub boundaries.
- **Queue Reorder**: `EFFECT_HEAVY_CLICK` upon pick up and drop.
