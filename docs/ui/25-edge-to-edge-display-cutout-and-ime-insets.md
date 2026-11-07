# Edge-to-Edge Display Cutout Inset and WindowInsets IME Handling

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **edge-to-edge display cutout inset and windowinsets ime handling** within the Nocturne application.

## Key Principles & Invariants
- Employs enableEdgeToEdge() API to render content seamlessly beneath system status and navigation bars.
- Applies WindowInsets.safeDrawing and WindowInsets.displayCutout paddings to critical UI controls.
- Dynamically adjusts bottom padding when on-screen IME keyboard expands in search views.
- Adapts navigation bar contrast dynamically according to background surface brightness.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
