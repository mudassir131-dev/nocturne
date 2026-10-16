# DSP 09: Bandlimited Sinc Interpolation for Sample Rate Conversion

## Core Engine
- High-quality resampler converts non-standard stream sample rates (44.1kHz, 96kHz) to device native 48kHz.
- Anti-aliasing lowpass filter prevents Nyquist frequency foldover distortion.
