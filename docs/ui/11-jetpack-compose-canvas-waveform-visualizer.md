# UI 11: Hardware-Accelerated Compose Canvas Live Waveform Visualizer

## Waveform Canvas
- Draws real-time 60fps audio waveform and spectrum bars using Jetpack Compose DrawScope.
- Employs pre-allocated Path and FloatArray buffers to eliminate GC allocations per frame.

