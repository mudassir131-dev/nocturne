# DSP 05: Triangular PDF Dithering & Psychoacoustic Noise Shaping

## Processing
- Applied when quantizing 32-bit float output to 16-bit hardware audio sinks.
- Eliminates harmonic distortion by shifting quantization noise floor above 15kHz.
