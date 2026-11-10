# FLAC Metadata Vorbis Comment and Picture Block Parser

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **flac metadata vorbis comment and picture block parser** within the Nocturne application.

## Key Principles & Invariants
- Native and Kotlin parser extracting METADATA_BLOCK_HEADER and VORBIS_COMMENT tags from local and streamed FLAC files.
- High-performance extraction of embedded album cover images from METADATA_BLOCK_PICTURE blocks without buffering entire audio streams.
- Support for custom vendor strings, multi-value artist tags, and replaygain peak and loudness normalization tags.
- Cue sheet and seektable block extraction enabling instant random-access seeking within monolithic multi-track FLAC files.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
