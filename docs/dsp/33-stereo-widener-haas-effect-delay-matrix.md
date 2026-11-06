# Stereo Widener Mid-Side Processing and Haas Effect Delay Matrix

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **stereo widener mid-side processing and haas effect delay matrix** within the Nocturne application.

## Key Principles & Invariants
- Mid-Side (M/S) decomposition: Mid = (L + R) * 0.5, Side = (L - R) * 0.5.
- Side channel gain boost and subtle Haas effect delay (10-25ms) added to lateral components.
- Perfect mono compatibility: Side signal cancels out completely when summed to mono.
- Enrichment of stereo soundstage width for headphones and car audio systems.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
