# MediaRouter Remote Cast Audio Device Discovery and Routing

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **media router remote cast audio device discovery and routing** within the Nocturne application.

## Key Principles & Invariants
- Discovers available Google Cast, DLNA, and UPnP endpoints via MediaRouteSelector.
- Negotiates direct stream URL handoff for high-definition lossless flac streaming to cast devices.
- Synchronizes remote play/pause/seek controls with local UI state and notification bar.
- Automatically falls back to local speaker output on remote device disconnection.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
