# Vorbis Comment and ID3v2 Tag Sanitization and Encoding Handling

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **vorbis comment and id3v2 tag sanitization and encoding handling** within the Nocturne application.

## Key Principles & Invariants
- UTF-8 and UTF-16 encoding auto-detection and normalization for embedded track tags.
- Sanitization of control characters, malformed null terminators, and trailing whitespace.
- Support for multi-value artist tags and genre delimiters (semicolon, slash, null).
- Resilience against maliciously crafted ID3v2 header sizes and tag bombs.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
