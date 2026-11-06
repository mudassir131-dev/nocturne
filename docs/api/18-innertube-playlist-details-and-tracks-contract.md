# InnerTube Playlist Metadata Header and Track Item List Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **innertube playlist metadata header and track item list contract** within the Nocturne application.

## Key Principles & Invariants
- RPC endpoint: `POST /youtubei/v1/browse` with playlist browseId (VLPL... / RDCLAK...).
- Extraction of playlist title, author, description, track count, and total duration.
- Parsing of `musicResponsiveListItemRenderer` elements into structured Song items.
- Support for user private playlists via authenticated cookie headers.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
