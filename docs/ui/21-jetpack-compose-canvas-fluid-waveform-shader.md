# Jetpack Compose Canvas Dynamic Fluid Waveform AGSL Shader

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **jetpack compose canvas dynamic fluid waveform agsl shader** within the Nocturne application.

## Key Principles & Invariants
- Renders real-time audio reactive fluid waves using Android Runtime Shader (AGSL) on API 33+.
- Animates Bezier curve control points modulated by audio frequency band amplitudes.
- Fallback to Canvas drawPath on older Android versions with zero dropped UI frames.
- Respects device battery saver state by reducing frame rate to 30 FPS when power saving is active.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
