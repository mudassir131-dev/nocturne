# Android FileProvider Content URI Sandboxing and Grant Flags

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **android fileprovider content uri sandboxing and grant flags** within the Nocturne application.

## Key Principles & Invariants
- Encapsulates exported audio, playlist, and log files using androidx.core.content.FileProvider.
- Replaces raw file:// filesystem paths with scoped content:// URIs with temporary grant flags.
- Explicitly limits permission grants to FLAG_GRANT_READ_URI_PERMISSION for sharing intents.
- Restricts accessible paths within file_paths.xml to prevent arbitrary file exposure.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
