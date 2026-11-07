# InnerTube MockWebServer Playback Endpoint Response Corpus

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **innertube mockwebserver playback endpoint response corpus** within the Nocturne application.

## Key Principles & Invariants
- Sets up OkHttp MockWebServer fixture with recorded JSON responses for browse, search, and next endpoints.
- Simulates HTTP 403, 429, and 500 error scenarios to verify exponential backoff and retry behavior.
- Asserts visitor data cookie extraction and rotation when new tokens are provided by mock server.
- Runs fully offline without hitting live Google servers during CI/CD test passes.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.testing`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
