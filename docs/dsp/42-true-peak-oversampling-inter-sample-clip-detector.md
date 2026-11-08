# True Peak Oversampling Inter Sample Clip Detector

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **true peak oversampling inter sample clip detector** within the Nocturne application.

## Key Principles & Invariants
- 4x oversampling polyphase FIR filter reconstructing continuous analog waveform peaks per ITU-R BS.1770 standards.
- Detects hidden inter-sample overshoots exceeding 0.0 dBFS caused by lossy AAC and MP3 decompression.
- Feeds peak overshoot value to lookahead limiter for transparent sub-sample gain reduction.
- Low-overhead polyphase filter bank implementation requiring only 12 MAC operations per oversampled sub-sample.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
