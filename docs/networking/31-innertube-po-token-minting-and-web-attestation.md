# InnerTube PO-Token Minting and Web Attestation Flow

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **innertube po-token minting and web attestation flow** within the Nocturne application.

## Key Principles & Invariants
- Implements Web Player attestation handshake for proof-of-origin (PO) token generation.
- Caches validated tokens in persistent keystore with TTL matching Google auth lifetime.
- Attaches valid PO tokens to streaming URL query params to circumvent bot blockages.
- Seamless headless regeneration when playback returns HTTP 403 Forbidden.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.network`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
