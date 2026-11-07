# InnerTube Chunked DataSource Byte-Range Request Slicing

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **innertube chunked datasource byte-range request slicing** within the Nocturne application.

## Key Principles & Invariants
- Splits large media streams into 2 MB - 5 MB byte-range requests for progressive buffering.
- Minimizes memory footprint and prevents whole-file download waste during quick song skipping.
- Verifies HTTP 206 Partial Content response codes and Content-Range response headers.
- Handles stream URL expiration timestamps by transparently refreshing playback URLs.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.network`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
