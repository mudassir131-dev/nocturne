# Adaptive Two-Pane Scaffold for Foldable and Tablet Dual Panels

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **adaptive two-pane scaffold for foldable and tablet dual panels** within the Nocturne application.

## Key Principles & Invariants
- Inspects WindowWidthSizeClass to toggle between single-column phone layout and expanded two-pane view.
- Displays full-screen player and lyrics sheet side-by-side on tablets and unfolded foldables.
- Adjusts panel proportion split (40% player / 60% queue & lyrics) for optimal horizontal ergonomics.
- Seamlessly reflows layout on runtime device orientation change without destroying playback state.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
