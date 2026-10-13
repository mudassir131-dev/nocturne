# Architecture Note 01: Nocturne System Overview and Modular Multi-Project Topology

## Overview
Architectural overview of Nocturne as a modern Android music platform. Documents multi-module Gradle topology including app, innertube, audio, betterlyrics, lrclib, kugou, paxsenixlyrics, kizzy, lastfm, unison, and canvas modules.

## Key Architectural Principles
1. Enforce strict bounded memory consumption and non-blocking asynchronous execution.
2. Maintain clean separation between presentation (Compose), domain logic, and data/native adapters.
3. Implement deterministic state machines for playback, caching, and network resilience.
4. Deliver audiophile-grade sound quality via dedicated 32-bit floating-point DSP pipelines.

## Platform Guarantees
- 60/120fps smooth UI rendering with zero frame drops during background loading.
- Bit-accurate audio playback with low-latency hardware buffer coordination.
