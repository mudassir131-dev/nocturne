# Obfuscation and R8 Dictionary Mapping Integrity Protection

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **obfuscation and r8 dictionary mapping integrity protection** within the Nocturne application.

## Key Principles & Invariants
- Multi-stage ProGuard and R8 rule tuning ensuring aggressive class, method, and field identifier obfuscation in release builds.
- Strict preservation boundaries for JNI entry points, Room entity classes, and kotlinx.serialization models.
- Automated build-time mapping.txt generation, archiving, and symbol verification across continuous integration.
- Dead-code elimination and repackaging protecting proprietary ALAC addon negotiation protocols against static disassembly.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
