# CDN Failover and Chunked Stream Retry Backoff Policy

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **cdn failover and chunked stream retry backoff policy** within the Nocturne application.

## Key Principles & Invariants
- Multi-origin CDN failover switching between primary YouTube edge nodes and secondary cached mirrors.
- Dynamic chunk retry policy with exponential backoff on HTTP 500, 502, 503, and 504 gateway errors.
- Preservation of partially downloaded audio byte ranges preventing redundant bandwidth consumption upon retry.
- Health scoring metric de-prioritizing problematic CDN IP endpoints experiencing persistent packet loss.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.networking`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
