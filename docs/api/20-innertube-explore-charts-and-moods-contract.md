# InnerTube Explore Mood Genres and Top Charts Endpoint Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **innertube explore mood genres and top charts endpoint contract** within the Nocturne application.

## Key Principles & Invariants
- RPC endpoint: `POST /youtubei/v1/browse` with browseId `FEmusic_charts` and `FEmusic_moods_and_genres`.
- Parsing of trending top 100 songs, global music videos, and genre category carousels.
- Localization support passing client language (hl) and country (gl) parameters.
- Mapping into Nocturne Home screen discovery sections.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
