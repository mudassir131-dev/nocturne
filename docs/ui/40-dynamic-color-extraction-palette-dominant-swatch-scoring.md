# Dynamic Color Extraction Palette Dominant Swatch Scoring

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **dynamic color extraction palette dominant swatch scoring** within the Nocturne application.

## Key Principles & Invariants
- AndroidX Palette analysis extracting dominant, vibrant, and muted swatches from album cover artwork.
- Custom scoring algorithm penalizing muddy grays and extreme bright whites to select optimal thematic accent colors.
- Dynamic luminance verification ensuring extracted text and icon colors meet WCAG AA contrast ratio (4.5:1).
- Caches extracted color palettes in Room database keyed by album art URL for instantaneous recall during track transitions.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
