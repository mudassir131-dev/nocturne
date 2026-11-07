# Media Notification Artwork Bitmap Memory Downsampling

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **media notification artwork bitmap memory downsampling** within the Nocturne application.

## Key Principles & Invariants
- Scales notification artwork down to standard 512x512 px to prevent Android IPC transaction size limits.
- Decodes images directly into ARGB_8888 or HARDWARE bitmaps via Coil image pipeline.
- Recycles temporary uncompressed bitmap memory on background rendering threads.
- Provides smooth cross-fade animation when track artwork changes in foreground notification.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
