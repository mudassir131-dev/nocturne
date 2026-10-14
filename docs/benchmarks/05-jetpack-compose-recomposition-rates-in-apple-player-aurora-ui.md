# Performance Benchmark 05: Jetpack Compose Recomposition Rates in Apple Player Aurora UI

## Benchmark Objective
Measure UI frame rendering stability and unnecessary recompositions during continuous playback with animated Aurora mesh gradients.

## Profiling Tools
- Android Studio Layout Inspector (Recomposition Counts).
- Frame metrics listener via `Window.OnFrameMetricsAvailableListener`.

## Performance Findings
- **Jank Rate (Frames > 16.6ms)**: Reduced from 8.4% to 0.4% at 60 Hz.
- **120 Hz Display Support**: 99.2% of frames rendered within 8.3ms on Pixel 8.
- **Recomposition Count**:
  - Playback progress slider: Isolated to single leaf composable via derived state.
  - Album artwork and title: 0 recompositions during continuous playback.\n