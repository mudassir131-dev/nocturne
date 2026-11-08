# Lyrics Karaoke Inter-Word Timing Cubic Bezier Animator

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lyrics karaoke inter word timing cubic bezier animator** within the Nocturne application.

## Key Principles & Invariants
- Animates word-by-word karaoke text highlights using custom cubic-bezier timing curves (FastOutSlowInEasing).
- Smoothly transitions gradient color fill and blur radius across individual word glyphs matching audio timestamps.
- Automatic auto-scroll logic smoothly centering active lyric line with spring animation physics.
- Tap-to-seek interaction allowing users to jump directly to any lyric line playback position.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
