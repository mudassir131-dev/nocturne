# Persistent Room FTS5 Full Text Search Virtual Table Sync

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **persistent room fts5 full text search virtual table sync** within the Nocturne application.

## Key Principles & Invariants
- SQLite FTS5 virtual table indexing track titles, artist names, album titles, and synchronized lyrics text.
- Room database triggers (AFTER INSERT, AFTER UPDATE, AFTER DELETE) keeping FTS virtual table synchronized.
- Sub-string and prefix matching queries delivering instant search suggestions as user types in UI search bar.
- Unicode tokenizer with diacritic folding ensuring accent-insensitive searching (e.g. Beyonce matches Beyoncé).

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
