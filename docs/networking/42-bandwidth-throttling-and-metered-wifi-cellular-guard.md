# Bandwidth Throttling and Metered WiFi Cellular Guard

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **bandwidth throttling and metered wifi cellular guard** within the Nocturne application.

## Key Principles & Invariants
- Listens to Android ConnectivityManager.NetworkCallback tracking NET_CAPABILITY_NOT_METERED and roaming status.
- Automatically switches audio stream quality from Hi-Res Lossless (FLAC/ALAC) down to Medium AAC (128kbps) on metered networks.
- Prompts user confirmation before initiating batch offline playlist downloads on cellular connections.
- Respects Android OS System Data Saver mode by halting all non-essential background network sync workers.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.networking`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
