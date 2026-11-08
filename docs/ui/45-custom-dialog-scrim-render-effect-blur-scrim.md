# Custom Dialog Scrim Render Effect Blur Scrim

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **custom dialog scrim render effect blur scrim** within the Nocturne application.

## Key Principles & Invariants
- Android 12+ RenderEffect.createBlurEffect applied to window decor view behind dialogs.
- Elegant frosted-glass aesthetic for addon source selection, equalizer presets, and settings modal dialogs.
- Fallback semi-transparent black scrim on legacy Android devices (API < 31).
- Animated blur radius and opacity ramp synchronized with dialog enter and exit transitions.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
