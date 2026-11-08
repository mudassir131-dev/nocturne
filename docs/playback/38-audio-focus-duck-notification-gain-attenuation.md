# Audio Focus Duck Notification Gain Attenuation

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **audio focus duck notification gain attenuation** within the Nocturne application.

## Key Principles & Invariants
- Handles AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK by ramping master gain smoothly down to 20% (-14dB) over 250ms.
- Restores nominal 100% (0dB) gain smoothly over 500ms exponential curve upon receiving AUDIOFOCUS_GAIN.
- Prevents sudden audio clipping or pop artifacts through frame-accurate logarithmic gain coefficient interpolation.
- Respects user toggle for Pause on transient loss instead of ducking for voice navigation clarity.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
