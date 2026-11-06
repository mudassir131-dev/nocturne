# InnerTube Next Continuation Endpoint Contract and Pagination Token Parsing

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **innertube next continuation endpoint contract and pagination token parsing** within the Nocturne application.

## Key Principles & Invariants
- RPC endpoint: `POST /youtubei/v1/next` with continuation token payload.
- Parsing of `continuationContents.musicPlaylistShelfContinuation` response structures.
- Extraction of remaining playlist tracks, up-next recommendations, and radio seeds.
- Graceful handling of expired continuation tokens with automatic fallback re-querying.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
