# UI Spec 04: Modal Bottom Sheet Drag Velocity & Spring Dismissal Physics

## Implementation Rules
- Implemented via Material 3 `ModalBottomSheet` with nested scroll connection.
- Velocity threshold of > 1200.dp/sec triggers instantaneous dismissal regardless of drag distance.
- Haptic tick feedback emitted upon snapping to half-expanded or fully-expanded states.
