# Screen Recording Detection and Flag Secure Overlay Policy

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **screen recording detection and flag secure overlay policy** within the Nocturne application.

## Key Principles & Invariants
- WindowManager.LayoutParams.FLAG_SECURE application preventing screenshot capture and screencast display mirroring.
- Dynamic toggling of secure flag during display of private user cookies, OAuth tokens, and backup passwords.
- MediaProjection detection API listening for active screen capture sessions and alerting the user.
- Privacy curtain overlay rendering blurred backdrop in Android OS recent tasks and app switcher overview.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
