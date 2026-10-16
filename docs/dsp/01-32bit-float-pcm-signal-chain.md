# DSP 01: 32-Bit Floating Point PCM Audio Processing Signal Chain

## Architecture
- Raw decoded audio converted to IEEE 754 32-bit float PCM (`float32_t`).
- Eliminates fixed-point rounding artifacts and headroom clipping across multiple filter stages.
