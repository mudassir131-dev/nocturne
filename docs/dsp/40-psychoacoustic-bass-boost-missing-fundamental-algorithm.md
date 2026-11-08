# Psychoacoustic Bass Boost Missing Fundamental Algorithm

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **psychoacoustic bass boost missing fundamental algorithm** within the Nocturne application.

## Key Principles & Invariants
- Synthesizes 2nd and 3rd harmonics of sub-bass frequencies (30Hz - 80Hz) exploiting the human brain missing fundamental perception.
- Enables deep perceived bass response on small mobile speakers and budget earphones unable to reproduce sub-60Hz physically.
- High-pass shelving filter attenuates inaudible DC excursion to protect micro-speaker drivers from mechanical damage.
- Non-linear wave-shaping transfer function f(x) = x / (1 + |x|) producing warm even harmonics.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
