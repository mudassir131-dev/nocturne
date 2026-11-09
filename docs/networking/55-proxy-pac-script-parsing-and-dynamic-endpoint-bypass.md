# Proxy PAC Script Parsing and Dynamic Endpoint Bypass

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **proxy pac script parsing and dynamic endpoint bypass** within the Nocturne application.

## Key Principles & Invariants
- Proxy Auto-Configuration (PAC) script parsing engine evaluating FindProxyForURL() dynamically.
- Direct routing bypass for local LAN Unison peer traffic while routing streaming data through proxy tunnel.
- Support for HTTP, HTTPS, SOCKS4, and SOCKS5 proxy protocol specifications in PAC return strings.
- Fast in-memory host lookup cache accelerating URL evaluation without repeated script executions.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.networking`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
