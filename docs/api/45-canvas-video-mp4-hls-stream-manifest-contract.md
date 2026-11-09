# Spotify Canvas Video MP4 and HLS Stream Manifest Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **canvas video mp4 hls stream manifest contract** within the Nocturne application.

## Key Principles & Invariants
- Protobuf entity parsing of canvas response decoding looping vertical video canvas MP4 stream URI.
- Automatic aspect ratio normalization (9:16) and silent audio track striping for seamless background looping.
- Akamai and Cloudflare CDN origin fallback when primary Spotify canvas edge node returns HTTP 403 or 404.
- Local MP4 file cache caching canvas video files indexed by track canvas ID with strict 200MB size ceiling.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
