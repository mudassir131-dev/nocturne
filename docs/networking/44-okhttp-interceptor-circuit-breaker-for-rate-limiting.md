# OkHttp Interceptor Circuit Breaker for Rate Limiting

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **okhttp interceptor circuit breaker for rate limiting** within the Nocturne application.

## Key Principles & Invariants
- Custom OkHttp interceptor detecting HTTP 429 Too Many Requests and HTTP 503 Service Unavailable responses.
- Three-state circuit breaker: CLOSED (nominal), OPEN (fast-fail for 30s), HALF-OPEN (canary probe).
- Exponential backoff with jitter: backoff time capped at max limit with randomized decorrelation interval.
- Prevents IP banning and API rate-limit exhaustion during aggressive playlist imports or scrobble flushes.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.networking`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
