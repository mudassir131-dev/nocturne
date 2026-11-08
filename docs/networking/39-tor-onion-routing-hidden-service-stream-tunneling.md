# Tor Onion Routing Hidden Service Stream Tunneling

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **tor onion routing hidden service stream tunneling** within the Nocturne application.

## Key Principles & Invariants
- Optional SOCKS5 proxy integration routing all outbound media queries through local Orbot and Tor daemon (127.0.0.1:9050).
- Enforces remote DNS resolution through SOCKS5 handshake preventing DNS leakage to upstream ISP.
- Stream chunk buffer multiplier increased 3x to accommodate variable latency and jitter across Tor circuits.
- Tor circuit isolation isolating InnerTube API traffic from lyrics and album artwork CDN traffic.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.networking`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
