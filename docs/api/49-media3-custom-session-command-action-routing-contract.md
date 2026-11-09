# Media3 Custom Session Command Action Routing Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **media3 custom session command action routing contract** within the Nocturne application.

## Key Principles & Invariants
- Custom MediaSession.Command declarations for lossless quality toggling, lyrics visibility, and speed modulation.
- Two-way session command dispatching between UI composables and background MusicService.
- SessionCommand.RESULT_SUCCESS and error envelope propagation across IPC process boundaries.
- Backward compatibility adapter mapping Media3 custom commands to legacy Android MediaControllerCompat extras.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
