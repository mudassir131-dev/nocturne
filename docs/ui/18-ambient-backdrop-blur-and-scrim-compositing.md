# UI 18: RenderEffect Backdrop Blur & Gradient Scrim Composition

## Backdrop Blur
- Uses RenderEffect.createBlurEffect on Android 12+ (API 31) for smooth album art glassmorphism.
- Gracefully falls back to high-performance pre-blurred bitmap sampling on older Android versions.

