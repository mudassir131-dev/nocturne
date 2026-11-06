# Last.fm Scrobbler Authentication Token Handshake and Session Key Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **last.fm scrobbler authentication token handshake and session key contract** within the Nocturne application.

## Key Principles & Invariants
- Web authentication flow obtaining unauthorized request token via browser redirect.
- Session exchange endpoint: `auth.getSession` returning permanent 32-character session key.
- Secure persistence of session key in encrypted preferences.
- Handling user logout and invalid session token invalidation.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
