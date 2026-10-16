# UI Spec 01: Dynamic Aurora Gradient Palette Extraction

## Architecture
Nocturne's Aurora UI engine dynamically samples dominant, vibrant, and muted color swatches from active track album artwork using `androidx.palette.graphics.Palette`.

## Contrast & Luminance Guardrails
- Minimum contrast ratio of 4.5:1 against text labels.
- Luminance clamping ensures dark mode background surfaces remain deep and non-fatiguing.
- Smooth HSV interpolation across track transitions over 600ms duration.
