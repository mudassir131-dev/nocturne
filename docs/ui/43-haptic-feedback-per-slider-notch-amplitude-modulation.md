# Haptic Feedback Per Slider Notch Amplitude Modulation

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **haptic feedback per slider notch amplitude modulation** within the Nocturne application.

## Key Principles & Invariants
- Integrates Android Vibrator and VibrationEffect.createPrebaked() for subtle tactile feedback on seek and volume sliders.
- Dynamic notch detection firing micro-vibration ticks whenever slider thumb crosses milestone intervals.
- Amplitude-modulated tick intensity scaling with drag velocity to provide realistic mechanical dial tactile sensation.
- Respects system-wide haptic feedback setting and user-configurable haptic toggle in app preferences.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
