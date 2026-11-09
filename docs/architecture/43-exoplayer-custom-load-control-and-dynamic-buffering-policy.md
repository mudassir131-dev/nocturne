# ExoPlayer Custom Load Control and Dynamic Buffering Policy

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **exoplayer custom load control and dynamic buffering policy** within the Nocturne application.

## Key Principles & Invariants
- Adaptive buffer window scaling from 15 seconds on metered mobile data up to 120 seconds on unmetered Wi-Fi.
- Minimum playback buffer threshold of 2,500ms preventing audio stutter upon track start and seek.
- Priority buffer allocation ensuring primary audio stream never starves behind companion artwork downloads.
- Dynamic back-buffer retention maintaining 30 seconds of historical audio for zero-latency rewind actions.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.architecture`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
