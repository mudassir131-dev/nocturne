# CueSheet Index Parser and Track Boundary Split Specification

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **cuesheet index parser and track boundary split specification** within the Nocturne application.

## Key Principles & Invariants
- Parsing of TRACK, INDEX 00 (pre-gap), INDEX 01 (start), TITLE, and PERFORMER commands.
- Frame to millisecond conversion formula: ms = (minutes * 60 + seconds) * 1000 + (frames * 1000 / 75).
- Single-file audio stream splitting into distinct Room database track entities.
- Full Unicode support for CUE sheet metadata encoded in UTF-8 or ANSI codepages.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.specs`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
