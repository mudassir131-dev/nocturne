# Playback Queue Item Reorder Drag Shadow and Elevation Animation

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **playback queue item reorder drag shadow and elevation animation** within the Nocturne application.

## Key Principles & Invariants
- Provides drag-and-drop playlist reordering within LazyColumn using item placement animations.
- Elevates dragged item with 8.dp shadow and 1.05x subtle scale spring to indicate active drag state.
- Delivers haptic feedback tick upon reorder item pickup and drop confirmation.
- Commits newly ordered queue indices to database atomically once drag gesture ends.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
