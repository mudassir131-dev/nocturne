# Scoped Storage MediaStore Indexing and Permission Audit

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **scoped storage media store indexing and permission audit** within the Nocturne application.

## Key Principles & Invariants
- Android 10+ Scoped Storage compliance utilizing ContentResolver queries on MediaStore.Audio.Media.
- READ_MEDIA_AUDIO runtime permission check and graceful degradation when storage access is denied.
- Local audio file metadata extractor reading ID3, Vorbis, and MP4 tags without requiring full file copies.
- ContentObserver registration listening for system-wide music file additions, deletions, and modifications.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
