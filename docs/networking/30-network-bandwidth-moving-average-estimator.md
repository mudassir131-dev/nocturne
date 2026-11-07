# Sliding Window Exponential Moving Average Bandwidth Estimator

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **sliding window exponential moving average bandwidth estimator** within the Nocturne application.

## Key Principles & Invariants
- Calculates network transfer speed using sliding window exponential moving average (EMA).
- Discards initial transfer warmup bytes to avoid skewed bandwidth measurements.
- Provides real-time bitrate recommendations to ExoPlayer AdaptiveTrackSelection.
- Emits reactive network quality tier (POOR, MODERATE, EXCELLENT) to UI components.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.network`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
