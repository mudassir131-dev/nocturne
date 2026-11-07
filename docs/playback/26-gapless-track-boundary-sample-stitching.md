# Gapless Track Boundary Sample Stitching and Zero-Pad Trimming

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **gapless track boundary sample stitching and zero-pad trimming** within the Nocturne application.

## Key Principles & Invariants
- Reads encoder delay and padding samples from metadata atoms (iTunSMPB / Vorbis Comments).
- Seamlessly trims preroll and postroll silence before pushing PCM audio buffers to AudioTrack.
- Pre-buffers the next playlist item's audio codec headers to eliminate decoder reinitialization lag.
- Guarantees sample-accurate continuity across continuous album transitions.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
