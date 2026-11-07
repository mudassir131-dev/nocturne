# JaCoCo Multi-Module Code Coverage Merged XML Report Pipeline

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **jacoco multi-module code coverage merged xml report pipeline** within the Nocturne application.

## Key Principles & Invariants
- Merges unit test and Android instrumentation test coverage execution files (*.exec, *.ec).
- Aggregates coverage metrics across app, innertube, kugou, lrclib, lastfm, and betterlyrics.
- Generates unified XML and HTML reports highlighting uncovered branches and lines.
- Enforces minimum 80% line coverage gate on core playback and audio processing modules.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.testing`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
