# Troubleshooting Guide 11: Fixing JNI DSP Buffer Clipping and Floating-Point Audio Distortion

## Symptom Overview
Users experience harsh acoustic clipping, distortion, or crackling artifacts when high-gain EQ presets (such as Bass Boost) are applied.

## Root Cause
Positive gain applied across multiple biquad peaking filters can cause 32-bit floating point PCM values to exceed the `[-1.0f, 1.0f]` normalized dynamic range before being written to the 16-bit DAC sink.

## Diagnostic Steps
1. Enable native DSP debug logs:
   ```bash
   adb logcat -s NocturneNativeDsp:V
   ```
2. Inspect peak amplitude output:
   `DSP Peak: +3.2 dBFS (CLIPPING DETECTED)`.

## Solution: Auto-Preamp & Soft Limiting
- Apply automatic pre-amplification attenuation equal to `-max(0.0f, maxBandGainDb)`:
  ```cpp
  float preampLinear = powf(10.0f, -highestGainDb / 20.0f);
  buffer[i] *= preampLinear;
  ```
- Implement a tanh soft-knee saturation limiter to gracefully attenuate transient overages without square-wave clipping.\n