# FLAC Vorbis Comment and ID3v2 Tag Dual Writing Integrity

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **flac vorbis comment and id3v2 tag dual writing integrity** within the Nocturne application.

## Key Principles & Invariants
- Writes Vorbis comment metadata blocks for FLAC containers and ID3v2.4 tags for MP3/AAC streams.
- Embeds album cover art directly into METADATA_BLOCK_PICTURE chunks with mime-type tags.
- Atomic file rename ensures audio files are never corrupted if write process is interrupted.
- Preserves high-resolution audio stream samples intact during metadata tagging.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
