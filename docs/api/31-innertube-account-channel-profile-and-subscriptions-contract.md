# InnerTube Account Channel Profile and Subscriptions Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **innertube account channel profile and subscriptions contract** within the Nocturne application.

## Key Principles & Invariants
- Account channel profile endpoint: `POST /youtubei/v1/browse` with `browseId: FEchannels` and authorized SAPISIDHASH bearer token.
- Channel item metadata parser extracts subscriber count badges, avatar thumbnails, banner URLs, and channel handle identifiers.
- Subscription feed pagination via continuation token exchange, returning batched channels with optimistic local caching.
- Secure token handling ensuring visitor data and OAuth authorization cookies are injected per HTTP request header.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
