# Service Locator and Dagger Hilt Dependency Injection Boundary

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **service locator and dagger hilt dependency injection boundary** within the Nocturne application.

## Key Principles & Invariants
- Architectural partition between compile-time Hilt singletons and dynamic runtime addon service locators.
- Lifecycle-scoped component containers ensuring audio engine state terminates cleanly with background service.
- Assisted injection factory patterns for dynamic media item player controller instances.
- Interface segregation ensuring mock implementations can be effortlessly substituted during automated tests.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.architecture`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
