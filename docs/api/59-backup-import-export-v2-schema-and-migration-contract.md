# Backup Import Export V2 Schema and Migration Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **backup import export v2 schema and migration contract** within the Nocturne application.

## Key Principles & Invariants
- JSON archive format specification for exporting Nocturne user data (playlists, song metadata, EQ presets, settings).
- Versioned header envelope with semantic versioning tag (version: 2) and SHA-256 integrity checksum.
- Backward compatibility parser gracefully ingesting legacy V1 JSON exports and upgrading to V2 relational entities.
- Optional GZIP compression layer reducing backup file sizes for Cloud Storage and Google Drive backups.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
