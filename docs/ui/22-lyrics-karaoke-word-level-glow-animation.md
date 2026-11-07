# Lyrics Karaoke Word-by-Word Smooth Glow Text Highlighting

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lyrics karaoke word-by-word smooth glow text highlighting** within the Nocturne application.

## Key Principles & Invariants
- Interpolates text color and shadow radius word-by-word matching syllable timestamp data.
- Applies smooth cubic easing curves across active line transitions for Apple Music-like aesthetics.
- Auto-scrolls synced lyrics to center the active vocal line smoothly in the viewport.
- Prevents user manual scroll collisions with 3-second auto-resume timer.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
