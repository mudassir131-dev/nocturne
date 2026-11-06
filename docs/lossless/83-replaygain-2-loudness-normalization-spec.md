# ReplayGain 2.0 Loudness Normalization and Peak Headroom Calculation

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **replaygain 2.0 loudness normalization and peak headroom calculation** within the Nocturne application.

## Key Principles & Invariants
- ITU-R BS.1770-4 LKFS integrated loudness measurement parsing from metadata tags.
- Automatic track-gain and album-gain application with selectable reference targets (-14 LUFS / -18 LUFS).
- Peak headroom limiting preventing clipping on aggressive positive gain boosts.
- Smooth transition between tracks with varying baseline recording volumes.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
