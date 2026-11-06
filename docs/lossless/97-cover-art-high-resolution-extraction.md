# Embedded FLAC Picture Block Extraction and Memory-Safe Decoding

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **embedded flac picture block extraction and memory-safe decoding** within the Nocturne application.

## Key Principles & Invariants
- Direct parsing of FLAC METADATA_BLOCK_PICTURE structures and APIC frames.
- Downsampling and memory-safe bitmap decoding using Android BitmapFactory.Options.inSampleSize.
- Hardware-accelerated rendering using Coil ImageLoader bitmap pool.
- Extraction of dominant color palettes for dynamic UI theming without main-thread stalls.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
