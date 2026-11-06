# Coil Image Loader LRU Disk and Memory Cache Sizing Policy

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **coil image loader lru disk and memory cache sizing policy** within the Nocturne application.

## Key Principles & Invariants
- Dynamic memory cache sizing allocated as 25% of available application heap memory.
- Disk cache configured with 250MB limit and least-recently-used file eviction policy.
- Fast downsampling of album art to exact screen pixel dimensions before memory insertion.
- Zero image decoding delays on high-density AMOLED display viewports.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.architecture`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
