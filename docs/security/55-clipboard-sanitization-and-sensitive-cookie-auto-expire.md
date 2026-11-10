# Clipboard Sanitization and Sensitive Cookie Auto Expire

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **clipboard sanitization and sensitive cookie auto expire** within the Nocturne application.

## Key Principles & Invariants
- Ephemeral clipboard management for imported YouTube cookies, OAuth tokens, and addon server API credentials.
- Usage of ClipDescription.EXTRA_IS_SENSITIVE flag on Android 13+ to mask sensitive clipboard previews in OS overlays.
- Coroutine-driven background timer scheduling automatic clipboard wiping 30 seconds after credential paste actions.
- In-memory credential scrubbing replacing byte arrays with zeros rather than relying on garbage collection.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
