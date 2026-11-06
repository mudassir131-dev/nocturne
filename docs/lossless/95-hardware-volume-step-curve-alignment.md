# Logarithmic Hardware Volume Step Curve Alignment for Hi-Res Output

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **logarithmic hardware volume step curve alignment for hi-res output** within the Nocturne application.

## Key Principles & Invariants
- Human auditory perception alignment using Weber-Fechner logarithmic volume curves.
- Expansion of Android standard 15/25 volume steps to fine-grained 100-step digital attenuator.
- Bit-exact gain calculation applied directly in 32-bit floating point PCM domain.
- Zero truncation noise on extremely low volume listening levels.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
