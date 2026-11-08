# SAF Persistent URI Permission Grant Refresh Worker

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **saf persistent uri permission grant refresh worker** within the Nocturne application.

## Key Principles & Invariants
- Takes and persists persistable URI permissions via ContentResolver.takePersistableUriPermission() for user-selected music folders.
- Periodic background worker queries ContentResolver.getPersistedUriPermissions() to identify revoked or dead tree URIs.
- Re-validates directory read and write access before starting local library scanning or audio file exports.
- Gracefully alerts user when an external SD card or USB OTG storage volume has been detached.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
