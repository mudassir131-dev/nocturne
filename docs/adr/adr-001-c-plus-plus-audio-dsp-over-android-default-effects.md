# ADR-001: Adopt Native C++ Audio DSP Over Android Standard AudioFX

## Status
Accepted

## Context
Android system AudioFX equalizer produces high latency, inconsistent vendor-dependent frequency responses, and unpredictable clipping across device manufacturers.

## Decision
Implement a proprietary 32-bit floating-point DSP pipeline in C++ via JNI (jni_bridge.cpp), bypassing platform AudioFX completely.

## Consequences
Guarantees bit-accurate, zero-latency, deterministic equalizer curves across 100% of Android devices without vendor fragmentation.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
