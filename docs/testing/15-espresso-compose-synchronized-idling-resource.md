# Testing 15: Espresso Idling Resource Synchronization with ExoPlayer State

## Test Synchronization
- Implements IdlingResource tracking ExoPlayer STATE_BUFFERING and STATE_READY transitions.
- Ensures Compose UI tests await media stream readiness without flaky Thread.sleep calls.

