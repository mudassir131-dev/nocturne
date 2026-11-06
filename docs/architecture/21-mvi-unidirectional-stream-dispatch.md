# MVI Unidirectional State Flow and ViewModel Event Dispatching

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **mvi unidirectional state flow and viewmodel event dispatching** within the Nocturne application.

## Key Principles & Invariants
- Unidirectional data flow with immutable ViewState, ViewEvent, and ViewEffect constructs.
- StateFlow and SharedFlow event dispatching ensuring single-time effect execution.
- Deterministic UI rendering decoupled from asynchronous business logic and background workers.
- Comprehensive unit testing patterns for ViewModel state reduction.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.architecture`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
