# Audio DSP Total Harmonic Distortion (THD+N) and SNR Automated Suite

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **audio dsp total harmonic distortion thd n and snr automated suite** within the Nocturne application.

## Key Principles & Invariants
- Generates 1 kHz pure sine wave test signals in 32-bit float PCM at 48 kHz sampling rate.
- Passes test signal through biquad EQ, dynamic compressor, and limiter filter chains.
- Measures FFT output spectrum to assert THD+N remains below 0.005% (-86 dB).
- Verifies zero unwanted phase cancellation or DC offset bias across output audio channels.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.testing`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
