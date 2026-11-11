# System Bar Insets Edge to Edge Gesture Navigation Padding

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **system bar insets edge to edge gesture navigation padding** within the Nocturne application.

## Key Principles & Invariants
- Full edge-to-edge window configuration drawing rich album art and visualizer behind status and navigation bars.
- WindowInsets.safeDrawing and displayCutout padding preventing interactive UI overlap with camera punch-holes.
- Dynamic status bar icon contrast switching (light/dark icons) based on luminance of underlying album art.
- Seamless support for predictive back gesture navigation margins along left and right display edges.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
