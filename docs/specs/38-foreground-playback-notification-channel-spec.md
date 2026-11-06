# Foreground Media Playback Notification Channel Priority and Action Buttons

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **foreground media playback notification channel priority and action buttons** within the Nocturne application.

## Key Principles & Invariants
- NotificationChannel importance: IMPORTANCE_LOW preventing intrusive audio dings on playback start.
- MediaStyle notification with compact action indices: Previous (0), Play/Pause (1), Next (2).
- Custom custom action buttons: Favorite toggle, Like/Dislike, and Loop mode controls.
- High-resolution large icon album art with circular crop on modern Android versions.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.specs`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
