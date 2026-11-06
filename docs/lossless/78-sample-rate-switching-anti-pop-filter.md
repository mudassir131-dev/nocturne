# Hardware Sample Rate Switching and Anti-Pop Ramp Attenuation

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **hardware sample rate switching and anti-pop ramp attenuation** within the Nocturne application.

## Key Principles & Invariants
- Smooth 10ms raised-cosine attenuation ramp applied prior to DAC sample rate reconfiguration.
- Prevention of speaker clicks and DC offset transients during track transitions.
- Support for 44.1kHz, 48kHz, 96kHz, and 192kHz native hardware clock switching.
- Hardware DAC synchronization state tracking.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
