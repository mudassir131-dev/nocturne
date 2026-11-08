# SAF Content Resolver Open File Descriptor Leak Prevention

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **saf content resolver open file descriptor leak prevention** within the Nocturne application.

## Key Principles & Invariants
- Enforces Kotlin use blocks on all ParcelFileDescriptor and InputStream handles obtained from ContentResolver.
- Prevents file descriptor exhaustion (Android FD limit of 1024 per process) during intensive local audio folder indexing.
- Sanitizes display names and path strings extracted from DocumentFile preventing directory traversal attacks.
- Validates MIME type against allowlist (audio/*, application/ogg, application/x-flac) before opening stream buffers.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
