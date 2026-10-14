# Subsystem API Specification 14: Aurora Player Color Extractor and Dynamic Gradient Token Contract

## Extraction Pipeline
1. Sample 128x128 bitmap downsampled from album artwork.
2. Run AndroidX Palette extraction to obtain:
   - Primary Dominant Color
   - Vibrant Swatch
   - Muted Dark Swatch
3. Enforce minimum contrast ratio of 4.5:1 against text labels.\n