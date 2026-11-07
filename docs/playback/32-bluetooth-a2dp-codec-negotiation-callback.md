# Bluetooth A2DP Codec Negotiation and Sample Rate Callback

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **bluetooth a2dp codec negotiation and sample rate callback** within the Nocturne application.

## Key Principles & Invariants
- Registers BluetoothProfile.ServiceListener to monitor BluetoothA2dp connection state changes.
- Queries active BluetoothCodecStatus to extract sampling frequency (44.1kHz, 48kHz, 96kHz) and bit depth.
- Configures internal resampler to match Bluetooth endpoint capabilities, avoiding duplicate resampling.
- Adjusts UI lossless indicator dynamically when audio is downsampled by Bluetooth SBC/AAC codecs.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
