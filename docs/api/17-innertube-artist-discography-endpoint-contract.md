# InnerTube Artist Discography Albums and Singles Browse Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **innertube artist discography albums and singles browse contract** within the Nocturne application.

## Key Principles & Invariants
- RPC endpoint: `POST /youtubei/v1/browse` with artist browseId (UC... / FEmusic_library...).
- Parsing of `musicShelfRenderer` containing Top Songs, Albums, Singles, and Related Artists.
- Extraction of album release year, track count, and high-resolution thumbnail images.
- Standardized mapping to domain ArtistDetails and Discography entities.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
