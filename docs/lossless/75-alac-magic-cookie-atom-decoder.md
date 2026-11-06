# ALAC Magic Cookie Atom Extraction and Channel Mask Configuration

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **alac magic cookie atom extraction and channel mask configuration** within the Nocturne application.

## Key Principles & Invariants
- Extraction of 24-byte 'alac' atom payload from MP4 container metadata structures.
- Configuration of native MediaCodec ALAC decoder with channel layout and frame length parameters.
- Support for multichannel channel masks and custom downmixing tables.
- Memory-safe parsing preventing out-of-bounds reads on corrupted MP4 atoms.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
