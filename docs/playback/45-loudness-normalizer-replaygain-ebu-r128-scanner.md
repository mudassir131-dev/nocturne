# Loudness Normalizer ReplayGain EBU R128 Scanner

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **loudness normalizer replaygain ebu r128 scanner** within the Nocturne application.

## Key Principles & Invariants
- Implements EBU R128 / ITU-R BS.1770-4 loudness measurement standard targeting -14 LUFS integrated loudness.
- Parses ReplayGain 2.0 tags (REPLAYGAIN_TRACK_GAIN, REPLAYGAIN_ALBUM_GAIN) from lossless metadata headers.
- Applies smooth preamp gain compensation with brickwall peak limiter to prevent inter-sample digital clipping.
- Dynamic loudness adjustment for un-tagged streams via fast sliding-window RMS loudness estimator.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
