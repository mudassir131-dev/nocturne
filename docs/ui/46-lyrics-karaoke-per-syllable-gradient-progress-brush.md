# Lyrics Karaoke Per Syllable Gradient Progress Brush

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lyrics karaoke per syllable gradient progress brush** within the Nocturne application.

## Key Principles & Invariants
- Custom Jetpack Compose Brush creating smooth left-to-right color wipe animation synchronized with syllable timestamps.
- Sub-pixel text rendering with soft glow shader effect accentuating currently sung karaoke word tokens.
- Dynamic color derivation sampling vibrant accents from album art to tint active karaoke lyric brushes.
- Hardware-accelerated canvas draw calls maintaining continuous 60fps/120fps refresh rate on high-refresh displays.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
