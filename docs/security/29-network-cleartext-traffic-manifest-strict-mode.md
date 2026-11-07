# Network Cleartext Traffic Prohibition and Manifest StrictMode Policy

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **network cleartext traffic prohibition and manifest strictmode policy** within the Nocturne application.

## Key Principles & Invariants
- Mandates android:usesCleartextTraffic=false across application manifest and network security config.
- Enforces system StrictMode network checks in debug builds to catch accidental HTTP requests.
- Disallows non-TLS traffic completely, requiring HTTPS / WSS for all remote communication.
- Provides explicit exemption only for user-defined local LAN addon IPs (e.g., 192.168.x.x).

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
