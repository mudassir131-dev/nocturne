# UI 16: Touch Helper Drag-and-Drop Reordering for Active Playback Queue

## Queue Gestures
- PointerInput gesture detector drives spring-animated drag handles in playback queue.
- Persists modified track order to Room database only upon drag release to reduce disk writes.

