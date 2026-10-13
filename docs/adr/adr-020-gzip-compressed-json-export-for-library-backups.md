# ADR-020: Adopt GZIP-Compressed JSON Format for Full Library Backup Archives

## Status
Accepted

## Context
Exporting raw SQLite database files caused compatibility crashes across different Room schema versions and architecture ABIs.

## Decision
Export library data into portable, human-readable JSON compressed with GZIP and verified with a SHA-256 integrity checksum.

## Consequences
Ensures 100% backward and forward compatibility for user library backups across all app versions.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
