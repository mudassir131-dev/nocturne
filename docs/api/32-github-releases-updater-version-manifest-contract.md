# GitHub Releases Updater Version Manifest Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **github releases updater version manifest contract** within the Nocturne application.

## Key Principles & Invariants
- OTA update check endpoint: `GET https://api.github.com/repos/mudassir131-dev/nocturne/releases/latest`.
- Semantic version parsing extracting tag_name, comparing against BuildConfig VERSION_NAME and VERSION_CODE.
- APK asset resolution verifying SHA-256 digest envelope before download trigger.
- Release changelog markdown parser rendering structured release notes in update modal dialog.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
