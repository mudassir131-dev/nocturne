# Playback Queue Shuffle Fisher Yates Fair Distribution

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **playback queue shuffle fisher yates fair distribution** within the Nocturne application.

## Key Principles & Invariants
- Cryptographically sound Fisher-Yates unbiased permutation algorithm producing uniform O(N) random order.
- Smart shuffle heuristic enforcing artist spacing constraint: no two tracks by the same artist within 4 consecutive items.
- Unshuffled original queue order preserved in memory for instantaneous non-destructive un-shuffle restoration.
- Dynamic queue insertion: newly queued user tracks are seamlessly injected without disrupting upcoming shuffled sequence.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
