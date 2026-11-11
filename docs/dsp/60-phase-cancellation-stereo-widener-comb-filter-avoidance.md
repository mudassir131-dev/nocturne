# Phase Cancellation Stereo Widener Comb Filter Avoidance

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **phase cancellation stereo widener comb filter avoidance** within the Nocturne application.

## Key Principles & Invariants
- Mid-side (M/S) stereo decorrelation algorithm enforcing strict mono-compatibility check.
- Phase correlation coefficient monitoring ensuring stereo image remains strictly above +0.3.
- All-pass dispersion network introducing subtle micro-delays without comb filtering phase notches.
- Automatic stereo foldback fallback when mono audio track format or single channel speaker is active.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
