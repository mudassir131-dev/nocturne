# Innertube Radio Seed and Automix Continuation Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **innertube radio seed and automix continuation contract** within the Nocturne application.

## Key Principles & Invariants
- Protocol contract for /youtubei/v1/next endpoint with RDAMVM automix playlist IDs for infinite continuous playback.
- Deserialization contract parsing PlaylistPanelVideoRenderer objects into Nocturne Track entities.
- Continuation parameter propagation tracking automix depth and preventing duplicate track recommendations.
- Radio seed payload generation supporting single song, artist radio, and genre radio variations.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
