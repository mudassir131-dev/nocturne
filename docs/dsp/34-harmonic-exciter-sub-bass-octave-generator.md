# Harmonic Exciter Non-Linear Polynomial Saturation and Warmth Generator

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **harmonic exciter non-linear polynomial saturation and warmth generator** within the Nocturne application.

## Key Principles & Invariants
- Soft-clipping polynomial saturation transfer function: f(x) = 1.5 * x - 0.5 * x^3 for |x| <= 1.0.
- Introduction of subtle second and third-order musical harmonics adding analog warmth.
- High-pass filtered sidechain ensuring harmonic generation only affects mid and high frequencies.
- Subtle acoustic excitation improving perceived clarity on compressed audio streams.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
