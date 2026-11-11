# Adaptive Layout Foldable and Tablet Two Pane Music Grid

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **adaptive layout foldable and tablet two pane music grid** within the Nocturne application.

## Key Principles & Invariants
- WindowSizeClass-driven adaptive layout engine supporting compact, medium, and expanded screen width regimes.
- Seamless two-pane layout presentation on foldable devices and tablets displaying queue alongside player.
- Folding posture awareness (tabletop mode, book mode) positioning interactive controls away from physical hinge crease.
- Smooth runtime configuration change handling preserving scroll positions and playback state during fold/unfold.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
