# Variable Pitch Preservation with SoundTouch Time Stretch

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **variable pitch preservation with soundtouch time stretch** within the Nocturne application.

## Key Principles & Invariants
- Integrated SoundTouch WSOLA (Waveform Similarity Overlap-Add) native C++ audio processing engine.
- Decoupled playback speed (0.25x to 2.0x) and musical pitch semitone shifting (-12 to +12 semitones).
- High-quality anti-aliasing sinc filter for phase coherence and transient preservation during speed modulation.
- Zero-allocation audio buffer processing within ExoPlayer AudioSink chain minimizing GC pauses.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
