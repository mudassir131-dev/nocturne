# Lossless FLAC Album Art Embedded Picture Block Cache

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lossless flac album art embedded picture block cache** within the Nocturne application.

## Key Principles & Invariants
- Direct byte-level extraction of METADATA_BLOCK_PICTURE from FLAC file headers without decoding audio frames.
- Generation of thumbnail-sized WebP downsampled images cached in dedicated disk image cache.
- Support for JPEG, PNG, and WebP embedded cover art formats up to 4096x4096 resolution.
- Invariant ensuring instant UI album art loading during offline high-resolution lossless playback.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
