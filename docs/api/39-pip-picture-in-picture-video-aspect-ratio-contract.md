# PiP Picture-in-Picture Video Aspect Ratio Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **pip picture in picture video aspect ratio contract** within the Nocturne application.

## Key Principles & Invariants
- Picture-in-Picture params builder configuring rational aspect ratio: 16:9 for landscape video, 1:1 for album art display.
- Auto-enter PiP mode on user leave hint when video playback or visualizer canvas is active.
- Remote action controls injected into PiP window: Play/Pause, Skip Next, and Like action triggers.
- Dynamic window boundary recalculation handling display rotation and multi-window split screen resizing.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
