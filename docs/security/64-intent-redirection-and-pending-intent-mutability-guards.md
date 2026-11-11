# Intent Redirection and Pending Intent Mutability Guards

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **intent redirection and pending intent mutability guards** within the Nocturne application.

## Key Principles & Invariants
- Explicit PendingIntent mutability flag enforcement (PendingIntent.FLAG_IMMUTABLE) on Android 12+.
- Verification and sanitization of incoming Intent target components to eliminate intent redirection vulnerabilities.
- Rejection of external intents containing non-standard bundle extras or serialized Parcelable objects.
- Whitelisting of internal broadcast actions for music player notification controls and headset key events.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
