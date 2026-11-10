# Biometric Prompt Cryptographic Key Cipher Authentication

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **biometric prompt cryptographic key cipher authentication** within the Nocturne application.

## Key Principles & Invariants
- Integration with AndroidX BiometricPrompt for authenticating access to sensitive backup archives and export keys.
- KeyStore initialization using KeyGenParameterSpec requiring setUserAuthenticationRequired(true) with hardware-backed StrongBox.
- Ephemeral Cipher object wrapped in BiometricPrompt.CryptoObject passed to onAuthenticationSucceeded callback.
- Automatic session key invalidation upon biometric sensor timeout, screen lock, or enrolled biometric alteration.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
