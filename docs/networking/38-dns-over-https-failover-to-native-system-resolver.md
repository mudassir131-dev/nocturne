# DNS over HTTPS Failover to Native System Resolver

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **dns over https failover to native system resolver** within the Nocturne application.

## Key Principles & Invariants
- Dual encrypted DoH resolvers (Cloudflare 1.1.1.1 and Quad9 9.9.9.9) providing ISP-level DNS censorship bypass.
- 3-second DNS query timeout with immediate failover to native Android InetAddress.getAllByName().
- Local DNS cache persisting resolved IP addresses for 3600 seconds with TTL decay tracking.
- Prevents DNS spoofing and SNI filtering while maintaining maximum connection reliability across diverse networks.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.networking`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
