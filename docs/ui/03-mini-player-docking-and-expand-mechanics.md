# UI Spec 03: Mini-Player Bottom Bar Docking & Expand Transition Curve

## Transition Dynamics
- Mini-player floats above bottom navigation bar with 8.dp elevation and 16.dp corner radius.
- Expand transition employs `FastOutSlowInEasing` path with coordinated alpha crossfade between mini controls and full playback controls.
