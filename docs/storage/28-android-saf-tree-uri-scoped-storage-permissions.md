# Android Storage Access Framework DocumentTree Scoped Storage Persist

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **android storage access framework documenttree scoped storage persist** within the Nocturne application.

## Key Principles & Invariants
- Requests user directory access via Intent.ACTION_OPEN_DOCUMENT_TREE on Android 11+ (API 30+).
- Persists Uri permissions across device reboots using contentResolver.takePersistableUriPermission.
- Reads and writes lossless audio files directly to user-selected external SD card or internal folder.
- Gracefully notifies user when target storage volume is unmounted or permissions are revoked.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
