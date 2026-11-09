# Audio Focus Transient Duck and Telephony Call Priority

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **audio focus transient duck and telephony call priority** within the Nocturne application.

## Key Principles & Invariants
- AudioManager.AUDIOFOCUS_GAIN and AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK handling logic.
- Smooth gain ducking reducing music playback volume to 20% (-14dB) during incoming notifications or navigation prompts.
- Immediate playback pause and state preservation upon incoming cellular or VoIP phone calls.
- Automatic volume and playback restoration when transient audio focus is abandoned by external apps.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
