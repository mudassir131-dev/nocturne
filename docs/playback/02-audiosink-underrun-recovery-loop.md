# Playback 02: AudioSink Buffer Underflow Recovery & Glitch Concealment

## Resilience
- Custom AudioSink listener detects underrun conditions on hardware write buffers.
- Pre-emptively feeds micro-comfort-noise or crossfades to avoid loud pop or click artifacts.
