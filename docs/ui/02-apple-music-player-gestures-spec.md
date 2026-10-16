# UI Spec 02: Apple Music Player Swipe Gestures & Fluid Fling Physics

## Gestural Architecture
- Vertical downward drag dismisses full player to docked mini-player.
- Horizontal swipe across artwork transitions between next and previous tracks in queue.
- Spring physics with critical damping ratio ($\zeta = 1.0$) eliminates bouncy oscillations.
