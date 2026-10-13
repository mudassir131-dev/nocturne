# Technical Specification 11: Last.fm Scrobble Batching and Session Token Protocol Specification

## 1. Scope & Objective
Specification of Last.fm 2.0 API interaction: track.updateNowPlaying and track.scrobble batch payloads, MD5 api_sig signature generation, and session validation.

## 2. Invariants & Requirements
- All operations must be deterministic, thread-safe, and resilient to sudden process termination.
- Data structures must adhere strictly to defined schema contracts and validate invariants on entry.
- Memory allocations must remain bounded to prevent OOM errors on low-RAM Android devices.

## 3. Conformance & Validation
- Validated via automated Kotlin unit tests and AndroidX test suites.
- Strict linting and static analysis conformance under Android Gradle Plugin checks.
