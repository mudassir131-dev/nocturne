# Dynamic Range Compression and Night Mode Gain Curves

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **dynamic range compression and night mode gain curves** within the Nocturne application.

## Key Principles & Invariants
- Nonlinear transfer function boosting quiet dialogue/whispers while limiting explosive audio transients.
- Dual-stage compressor curve with configurable knee width (3dB to 6dB) and 4:1 compression ratio above threshold.
- Attack time (15ms) and release time (250ms) tuned to prevent audible gain pumping artifacts.
- User-configurable Night Mode toggle optimizing listening experience in noise-sensitive environments.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
