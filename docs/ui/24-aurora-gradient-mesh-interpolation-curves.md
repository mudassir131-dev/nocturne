# Aurora Dynamic HSV Gradient Mesh Interpolation Math

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **aurora dynamic hsv gradient mesh interpolation math** within the Nocturne application.

## Key Principles & Invariants
- Extracts dominant color palette (Primary, Secondary, Ambient) from album artwork using AndroidX Palette.
- Blends multi-point color gradient mesh in HSV color space to prevent muddy intermediate gray tones.
- Continuously rotates gradient anchor points at low velocity for a living, ambient visual backdrop.
- Renders into offscreen hardware layer to preserve battery life and eliminate redraw penalties.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
