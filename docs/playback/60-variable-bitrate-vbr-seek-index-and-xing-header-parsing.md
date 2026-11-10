# Variable Bitrate VBR Seek Index and Xing Header Parsing

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **variable bitrate vbr seek index and xing header parsing** within the Nocturne application.

## Key Principles & Invariants
- Parsing engine reading Xing and Info tags within the first MP3 audio frame to detect variable bitrate encoding.
- Interpolation of the 100-byte TOC (Table of Contents) seek table providing accurate byte offsets for arbitrary seek targets.
- Calculation of accurate track duration from Xing frame count headers overcoming VBR duration estimation errors.
- Fallback linear interpolation seeking with sample position correction for corrupted or missing Xing header blocks.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
