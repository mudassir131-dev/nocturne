# Coroutine Dispatcher Concurrency and Threading Topology

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **coroutine dispatcher concurrency and threading topology** within the Nocturne application.

## Key Principles & Invariants
- Dedicated single-threaded Dispatchers.Audio exclusively reserved for playback command sequencing and DSP state.
- Dispatchers.IO bounded thread pool isolating disk I/O, database writes, and network downloads.
- MainImmediate dispatcher prioritizing UI event handling and frame-rate synchronized Compose state updates.
- Structured concurrency hierarchies with SupervisorJob preventing child failures from canceling entire player lifecycle.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.architecture`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
