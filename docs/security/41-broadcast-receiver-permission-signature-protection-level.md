# Broadcast Receiver Permission Signature Protection Level

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **broadcast receiver permission signature protection level** within the Nocturne application.

## Key Principles & Invariants
- Declares custom permission com.mudassir131.yt.permission.INTERNAL_IPC with protectionLevel signature.
- Secures internal playback control broadcasts and service state synchronization events against third-party snooping.
- Sets android:exported=false on all internal app receivers preventing malicious apps from spoofing playback events.
- Validates calling UID on incoming IPC intents via Binder.getCallingUid() before processing command payloads.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
