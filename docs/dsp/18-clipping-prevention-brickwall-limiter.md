# DSP 18: Zero-Latency Lookahead Brickwall Peak Limiter with Smooth Release

## Peak Limiting
- Lookahead buffer of 64 audio samples calculates signal peak trajectory prior to output stage.
- Applies exponential release smoothing curve to eliminate inter-sample peaks without pumping.

