# Synced Lyrics Offline Disk Store Binary Format and Compression

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **synced lyrics offline disk store binary format and compression** within the Nocturne application.

## Key Principles & Invariants
- Serializes synchronized lyrics lines and word-level timestamps into compact flat buffers.
- Employs Deflate / Snappy compression to store thousands of synced lyrics files in a few megabytes.
- Decodes lyrics on demand in less than 2 milliseconds for immediate display in the player sheet.
- Indexes lyrics by track ID and duration hash to resolve fast cache hits.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
