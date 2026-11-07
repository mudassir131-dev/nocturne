# Addon Manifest HTTP Host Domain Whitelisting and Firewall Bounds

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **addon manifest http host domain whitelisting and firewall bounds** within the Nocturne application.

## Key Principles & Invariants
- Validates user-entered addon URLs against strict URL schema and domain validation regex.
- Blocks RFC 1918 private IP ranges unless user explicitly enables Allow Local Network Addons.
- Disallows loopback (127.0.0.1 / localhost) and link-local cloud metadata endpoints.
- Sanitizes addon responses to prevent Cross-Site Scripting (XSS) or HTML injection in UI dialogs.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
