# SAF Scoped Storage Path Traversal and Symlink Injection Defense

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **saf scoped storage path traversal and symlink injection defense** within the Nocturne application.

## Key Principles & Invariants
- Validates file paths during import/export to block directory traversal attacks (../ sequences).
- Resolves canonical paths and confirms they reside strictly within designated application sandbox.
- Rejects symbolic links (symlinks) pointing to sensitive private directories.
- Enforces read-only verification before streaming audio files from external storage documents.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
