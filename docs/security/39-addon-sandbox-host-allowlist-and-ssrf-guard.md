# Addon Sandbox Host Allowlist and SSRF Guard

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **addon sandbox host allowlist and ssrf guard** within the Nocturne application.

## Key Principles & Invariants
- Server-Side Request Forgery (SSRF) validation filter rejecting addon endpoints pointing to private IP subnets.
- Blocks loopback requests to internal services unless explicitly confirmed by advanced developer toggle.
- Strict HTTPS scheme verification for third-party addon provider base URLs.
- Sanitizes addon manifest strings before rendering into Compose UI preventing script injection in addon title and description.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
