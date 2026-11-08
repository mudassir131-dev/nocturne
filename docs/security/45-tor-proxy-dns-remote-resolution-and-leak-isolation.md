# Tor Proxy DNS Remote Resolution and Leak Isolation

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **tor proxy dns remote resolution and leak isolation** within the Nocturne application.

## Key Principles & Invariants
- Configures custom OkHttp Dns implementation that delegates all domain resolution to the remote SOCKS5 proxy server.
- Prevents local DNS leaks where domain lookups accidentally bypass the Tor tunnel and expose user browsing activity to the ISP.
- Validates that no UDP packets escape the application boundary when Tor mode is activated.
- Immediate circuit disconnect and stream pause if the local SOCKS5 proxy connection drops.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
