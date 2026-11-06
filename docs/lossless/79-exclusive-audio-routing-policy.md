# Exclusive Audio Routing Policy for External USB DAC Endpoints

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **exclusive audio routing policy for external usb dac endpoints** within the Nocturne application.

## Key Principles & Invariants
- Bypass Android system mixer via direct AudioTrack offload and USB audio endpoints.
- Bit-perfect digital transport avoiding Android OS 48kHz resampling.
- Automatic fallback to standard routing upon physical USB disconnection.
- Support for asynchronous USB audio transfer mode.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
