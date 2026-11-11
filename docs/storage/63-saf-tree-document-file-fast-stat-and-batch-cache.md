# SAF Tree Document File Fast Stat and Batch Cache

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **saf tree document file fast stat and batch cache** within the Nocturne application.

## Key Principles & Invariants
- Elimination of slow DocumentFile.listFiles() round trips via raw ContentResolver query on SAF document tree URI.
- Batch retrieval of document ID, display name, MIME type, size, and last modified timestamps in single SQLite cursor.
- 20x performance speedup when indexing external SD card local music folders with 10,000+ tracks.
- In-memory stat cache with 60-second TTL preventing redundant content provider IPC overhead during scan.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
