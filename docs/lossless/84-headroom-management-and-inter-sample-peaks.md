# Inter-Sample True Peak Detection and Digital Headroom Management

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **inter-sample true peak detection and digital headroom management** within the Nocturne application.

## Key Principles & Invariants
- 4x oversampling true-peak analysis to detect hidden inter-sample overshoots.
- Dynamic headroom attenuation reserve (1.5dB) to guarantee zero DAC interpolation distortion.
- Compliance with EBU R128 and ITU-R BS.1770 true-peak ceiling recommendations.
- Real-time visual clipping indicators for user feedback in audiophile settings.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
