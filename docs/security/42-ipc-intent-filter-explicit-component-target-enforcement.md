# IPC Intent Filter Explicit Component Target Enforcement

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **ipc intent filter explicit component target enforcement** within the Nocturne application.

## Key Principles & Invariants
- Replaces implicit Intents with explicit Intent.setComponent() or Intent.setPackage(context.packageName).
- Eliminates intent hijacking vulnerabilities where rogue apps register matching intent filters to intercept sensitive data.
- Explicitly sets PendingIntent.FLAG_IMMUTABLE on all PendingIntents passed to notifications and widgets.
- Verifies action string against strict enum whitelist before dispatching service commands.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
