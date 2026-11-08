# Audio Tag Extraction and MP4 Box Metadata Reader

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **audio tag extraction and mp4 box metadata reader** within the Nocturne application.

## Key Principles & Invariants
- Custom low-overhead MP4 ISO base media file format (moov/udta/meta/ilst) parser written in pure Kotlin.
- Extracts title, artist, album, embedded artwork, and track number boxes with minimal memory overhead.
- Reads Vorbis comments and FLAC metadata blocks directly from locally downloaded lossless audio files.
- Stream-oriented reader inspecting only header boxes without reading multi-megabyte audio payload into memory.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
