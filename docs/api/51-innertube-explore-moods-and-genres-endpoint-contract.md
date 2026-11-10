# Innertube Explore Moods and Genres Endpoint Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **innertube explore moods and genres endpoint contract** within the Nocturne application.

## Key Principles & Invariants
- Specification for /youtubei/v1/browse endpoint queries requesting explore category grids and mood chips.
- Structured response parsing mapping SectionListRenderer and GridRenderer into normalized ExploreGenre models.
- Continuation token extraction and pagination for dynamic category shelf loading.
- Cache-control header handling and local TTL enforcement for mood and genre catalog listings.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
