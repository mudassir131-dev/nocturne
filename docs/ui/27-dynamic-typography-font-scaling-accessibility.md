# Dynamic Typography Font Scaling and Non-Linear Density Curves

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **dynamic typography font scaling and non-linear density curves** within the Nocturne application.

## Key Principles & Invariants
- Implements non-linear font scaling conforming to Android 14 large text accessibility standards.
- Prevents UI overflow and clipping by wrapping subtext and scaling artist names gracefully.
- Employs scalable sp units with bounded minimum and maximum constraints for core player buttons.
- Fully audited against Android Accessibility Scanner for screen reader accessibility labels.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
