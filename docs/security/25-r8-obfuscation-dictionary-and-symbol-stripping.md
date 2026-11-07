# R8 Obfuscation Dictionary and Native Symbol Stripping Rules

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **r8 obfuscation dictionary and native symbol stripping rules** within the Nocturne application.

## Key Principles & Invariants
- Configures aggressive R8 full-mode shrinking, optimization, and class/method obfuscation.
- Strips native C++ debug symbols and JNI function names from release shared libraries (.so).
- Strips source file names and line number attributes using ProGuard rules.
- Protects internal crypto algorithms and InnerTube decryption transforms from reverse engineering.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
