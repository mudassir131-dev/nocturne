# Sample Rate Switching and USB DAC Exclusive Mode Handoff

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **sample rate switching and usb dac exclusive mode handoff** within the Nocturne application.

## Key Principles & Invariants
- USB host mode peripheral detection and audio format capability interrogation via Android UsbManager.
- Direct PCM routing bypassing Android AudioFlinger software resampler to preserve bit-perfect fidelity.
- Automatic AudioTrack recreation upon source sample rate transitions (44.1kHz -> 96kHz -> 192kHz).
- Pop-and-click mitigation ramp applying 10ms micro-fade during hardware clock rate reconfiguration.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
