# Hardware Volume Keys Interception in Background Playback State

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **hardware volume keys interception in background playback state** within the Nocturne application.

## Key Principles & Invariants
- Binds STREAM_MUSIC audio stream volume changes to active MediaSession volume provider.
- Implements fine-grained volume steps (30 to 50 fractional increments) for audiophile volume curve.
- Suppresses standard volume overlay when custom in-app volume slider overlay is active.
- Ensures hardware button responsiveness even when screen is turned off or device is locked.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
