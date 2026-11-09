# Mini-Player Drag Resistance and Velocity Fling Animation

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **mini player drag resistance and velocity fling animation** within the Nocturne application.

## Key Principles & Invariants
- Jetpack Compose pointerInput gesture detector calculating touch drag displacement and velocity tracking.
- Physics-based spring animation (Spring.StiffnessMediumLow, Spring.DampingRatioLowBouncy) for bottom sheet docking.
- Non-linear drag resistance dampening overscroll movement when dragged beyond natural boundary limits.
- Seamless shared layout morphing expanding mini-player thumbnail into full-bleed album art hero element.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
