# FLAC StreamInfo Metadata Block Parser and Bit Depth Validation

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **flac streaminfo metadata block parser and bit depth validation** within the Nocturne application.

## Key Principles & Invariants
- Zero-copy parsing of the 34-byte mandatory FLAC STREAMINFO metadata block.
- Validation of minimum and maximum block sizes, sample rates up to 192kHz, and bit depths (16/24-bit).
- MD5 audio signature verification against decoded PCM frame data.
- Early rejection of malformed or truncated audio headers before player allocation.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
