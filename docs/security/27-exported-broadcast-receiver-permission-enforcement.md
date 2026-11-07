# Exported BroadcastReceiver Permission Enforcement and Component Gating

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **exported broadcastreceiver permission enforcement and component gating** within the Nocturne application.

## Key Principles & Invariants
- Mandates android:exported=false on all internal app BroadcastReceivers and Services.
- Uses signature-level custom permissions for companion plugins and extensions.
- Registers runtime receivers with RECEIVER_NOT_EXPORTED flag on Android 13+ (API 33+).
- Prevents malicious third-party apps from injecting fake media button or playback control intents.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
