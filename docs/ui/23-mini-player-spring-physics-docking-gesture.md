# Mini-Player Spring Physics Drag-to-Expand Docking Interaction

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **mini-player spring physics drag-to-expand docking interaction** within the Nocturne application.

## Key Principles & Invariants
- Implements natural physics-based drag interactions using Jetpack Compose Animatable and spring().
- Computes drag velocity thresholds to snap between collapsed mini-player and expanded full-screen player.
- Smoothly scales cover art dimensions and interpolates control button layouts during gesture drag.
- Intercepts swipe-down to dismiss mini-player and stop playback with haptic feedback.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
