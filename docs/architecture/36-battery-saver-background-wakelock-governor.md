# Android Doze Mode WakeLock Governance and Foreground Service Lifecycle

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **android doze mode wakelock governance and foreground service lifecycle** within the Nocturne application.

## Key Principles & Invariants
- Strict partial WakeLock lifecycle bound to ExoPlayer playback states (STATE_READY / PLAYING).
- Immediate release of WakeLocks upon pause, buffer exhaustion, or audio focus loss.
- Foreground service notification attachment satisfying Android 14+ background execution policies.
- Battery optimization guide dialogs for aggressive OEM battery killer architectures.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.architecture`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
