# Lyrics Karaoke Spring Physics Scroll Centering Animator

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lyrics karaoke spring physics scroll centering animator** within the Nocturne application.

## Key Principles & Invariants
- Jetpack Compose Animatable spring physics scroll controller centering active lyric line in viewport.
- Damped harmonic oscillator parameters (stiffness 300f, dampingRatio 0.85f) preventing jarring scroll snaps.
- Automatic manual scroll detection pausing auto-scroll for 4 seconds when user browses lyrics manually.
- Smooth resume transition re-centering playback position without disorienting visual jumps.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
