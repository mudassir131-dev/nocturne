# Audio Focus Transient Loss and Ducking Curve Configuration

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **audio focus transient loss and ducking curve configuration** within the Nocturne application.

## Key Principles & Invariants
- Gradual logarithmic gain attenuation down to -12 dB upon AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK.
- Smooth 250 ms cross-ramp transition avoiding sudden clicks or perception artifacts.
- Immediate pause handling on AUDIOFOCUS_LOSS_TRANSIENT with automatic resume on AUDIOFOCUS_GAIN.
- Thread-safe coordination between AudioManager callbacks and ExoPlayer volume multiplier.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
