# Unidirectional Data Flow and MVI State Machine Topology

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **unidirectional data flow and mvi state machine topology** within the Nocturne application.

## Key Principles & Invariants
- Architectural blueprint enforcing Unidirectional Data Flow (UDF) across all Jetpack Compose screens and ViewModels.
- Single source of truth managed via immutable UiState data classes exposed as StateFlow<UiState>.
- Processing of user interactions as sealed UiIntent actions mapped into state reducers without side-effect leaks.
- Discrete one-off UI events (snackbar messages, navigation triggers) modeled as distinct UiEffect shared channels.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.architecture`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
