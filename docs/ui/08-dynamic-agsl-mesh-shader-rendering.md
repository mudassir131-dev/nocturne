# UI Spec 08: AGSL Mesh Shader Hardware Acceleration for Ambient Background

## AGSL Shader Specs (Android 13+ / API 33+)
- Runtime shader computes continuous sinusoidal color warps based on artwork hue spectrum.
- Fallback for API < 33 renders multi-radial gradient blurs on RenderNode to prevent CPU overhead.
