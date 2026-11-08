# Network Security Config Cleartext Traffic Permits

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **network security config cleartext traffic permits** within the Nocturne application.

## Key Principles & Invariants
- Manifest-level android:networkSecurityConfig XML configuration enforcing strict HTTPS across all public network domains.
- Explicit exception permits cleartext HTTP only for local loopback addresses (127.0.0.1, localhost) for testing addons and proxies.
- Rejects all unauthorized plain-text HTTP connections to remote servers, preventing Wi-Fi man-in-the-middle packet eavesdropping.
- Configures custom trust anchors allowing user-installed CA certificates solely in debug builds for network traffic inspection.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
