# Opus OGG Packet Demuxing and Sample Granule Alignment

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **opus ogg packet demuxing and sample granule alignment** within the Nocturne application.

## Key Principles & Invariants
- Demuxing engine parsing Ogg pages, bitstream serial numbers, and segment tables for Opus audio streams.
- Header inspection reading OpusHead identification packets, channel counts, and pre-skip sample discard counts.
- Granule position calculation translating 48kHz audio clock timestamps into exact playback timeline positions.
- Seamless handling of chained Ogg streams and mid-stream bitrate adjustments during adaptive network streaming.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
