# Nocturne Changelog & Release Notes

## [v2.22.40] - 2026-11-04

### 🧭 New Navigation Bar (Default On)
- **Modern Floating Architecture**: Re-engineered the bottom navigation bar with an expressive floating pill container, enabled by default in Appearance Settings (`newNavbar = true`).
- **Fluid Morphing & Selection Physics**: Active tabs expand smoothly into an icon + text pill with spring dynamics and haptic touch, while inactive tabs remain compact and clean.
- **Interactive Three-Dots Floating Action Button**: Features a dedicated `VibrantFloatingActionButton` with an animated 90° rotation and icon morphing between `MoreVert` and `Close`.
- **Three Core Quick Actions Menu**:
  1. **Music Recognition** - Immediate navigation to the new dedicated audio recognition screen.
  2. **History** - Quick jump to playback history.
  3. **Stats** - Instant access to user listening statistics.

### 🎙️ Dedicated Online Music Recognition Engine
- **Powered by Shazam (Pure Kotlin)**: Integrated real-time audio recognition using pure Kotlin DejaVu fingerprinting algorithm (`ShazamSignatureGenerator`) and official Shazam endpoint resolution.
- **Zero API Keys Required**: Seamlessly captures 16kHz microphone audio on-device and resolves online track signatures with rate-limiting and exponential backoff retry.
- **Comprehensive Song Metadata**: Displays album artwork, verified title, artist, album name, primary genre, release date, and ISRC identifier.
- **Interactive Preview & Direct Actions**:
  - Live 6-line preview of recognized lyrics.
  - Record label and distributor attribution.
  - One-tap search in Nocturne's catalog to stream or add directly to library/playlists.
  - External launch buttons for Shazam and YouTube.
- **Expressive Visuals**: Animated pulsing listening orb with dual sound-wave ripple rings, status pills, and permission onboarding.

### 🖤 Appearance & Pure Black AMOLED Mode
- **Persistent Pure Black**: Fixed the regression where pure black mode was forced off by an extraneous launched effect in settings.
- **AMOLED-Optimized Surfaces**: Full support for true pitch-black (`#000000`) surfaces across the floating toolbar, quick action menus, and recognition screen.

### ⚙️ Engine, Build & Stability
- **Version Bump**: Bumped `versionCode` to `51` and `versionName` to `2.22.40`.

---

## [v2.22.39] - 2026-10-25

### 🎚️ Raw-Max Bitrate Playback
- **Every Mode Tries The Verified Catalog First**: OPUS included — per-song verified catalog matches (Saavn 320 kbps AAC, self-hosted/CD lossless, local FLAC/ALAC) now play instead of being locked to YouTube's ~160 kbps Opus ceiling.
- **Highest-Bitrate Fallback**: Songs without a catalog match still play the exact same `videoId` through YouTube, but the fallback now ranks candidates by the highest available bitrate (e.g. itag 141 AAC 256 kbps) with the mode's codec as a tie-break, instead of always pinning the smaller Opus.
- **Preflight Hardening**: Remote catalog probes use `Range: bytes=0-0` GET instead of HEAD, after live probes showed some CDNs reject HEAD and silently push playback to the 160 kbps fallback.
- **Exactness Uncompromised**: The per-song verification gates (title floor, artist verification, duration fingerprint, confidence threshold) are unchanged.

### ⚙️ Engine, Build & Stability
- **Version Bump**: Bumped `versionCode` to `50` and `versionName` to `2.22.39`.

---

## [v2.22.38] - 2026-10-16

### 📖 Architecture & Systems Documentation Hub
- **Comprehensive Modular Architecture**: Documented all 20 core subsystems including C++ JNI audio engine & DSP pipeline, Media3 ExoPlayer foreground lifecycle, InnerTube reverse-engineering protocol, and Jetpack Compose MVI state hoisting.
- **Architectural Decision Records (ADR 001 - 020)**: Formally captured architectural standards including native 32-bit float PCM audio pipeline, Android Keystore encryption for cookie sessions, two-pass fuzzy match heuristics, and SQLite WAL database persistence.
- **Production Troubleshooting Guides**: Detailed root-cause analysis and mitigation strategies for audio underruns, OEM background battery killers, Bluetooth A2DP latency, InnerTube HTTP 403 / rate limits, and audio noisy pauses.
- **Technical Specifications & Benchmarks**: Added formal specifications and benchmark analyses for DSP CPU utilization, database WAL query performance across 100k tracks, and audio streaming latency distribution.
- **Community & Contributor Guides**: Published guides for building Nocturne locally, compiling native C++ code with NDK, adding synced lyrics providers, and tuning track-matching thresholds.
- **Security Standards & RFC Roadmap**: Established strict zero-telemetry and key management policies, along with RFC proposals for Android Auto, Google Cast v3, and ReplayGain 2.0 loudness normalization.

### 📢 Broadcast & In-App Announcements
- **Updated Broadcast Channel**: Deployed latest broadcast announcement notifications and status synchronization for all Nocturne app users.

### ⚙️ Engine, Build & Stability
- **Version Bump**: Bumped `versionCode` to `49` and `versionName` to `2.22.38`.

---

## [v2.22.37] - 2026-10-08

### 🌟 Collapsing Top Bars Across Settings
- **Material 3 LargeTopAppBar Standardized**: Integrated `LargeTopAppBar` with dynamic `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()` across every settings screen:
  - Appearance Settings
  - Player and Audio Settings
  - Music Together Screen
  - Content Settings
  - Privacy Settings
  - Discord Settings
  - Integration Screen
  - Storage Settings
  - Backup & Restore Screen
  - Account Settings
  - About Screen
  - Main Settings Screen
- Headers now smoothly scale and collapse opposite to the back arrow on upward scroll.

### 🎨 Settings Redesign & Divider Lines
- **Grouped Card Layout**: Restyled the primary Settings hub into distinct, clean rounded card containers matching modern Material 3 guidelines.
- **Removed Unnecessary Items**: Streamlined settings by removing the redundant "AI Hub" and "Listening Summary" entries, giving "Account" a clean dedicated card.
- **Minimalist Bare Icons**: Removed bulky icon container boxes and unnecessary trailing chevron arrows for a distraction-free, elegant look.
- **Visible 1.dp Separators**: Implemented clearly defined horizontal separators (`thickness = 1.dp`, `alpha = 0.5f`) between list items in all inner settings groups (`PreferenceGroup`).
- **Comprehensive Separator Propagation**: Propagated visible separator lines into:
  - Default Player Queue
  - Apple Music Player Queue
  - Song History Screen
  - Search Discovery Hub

### 🔍 Search Section Enhancements
- **Instant Recent Search Screen**: Tapping or focusing the top search bar now immediately renders past search queries and history items without requiring query input.
- **Spotify & Apple Music Mood Playlists**: Synced real-time mood and vibe playlists from both Spotify ("Today's Top Hits", "Mood Booster", "Deep Focus", "Chill Vibes") and Apple Music ("Spatial Audio Hits", "Pure Motivation", "Today's Chill", "A-List Pop").
- **Google Dedicated Material Loading Indicator**: Integrated Material 3 `CircularProgressIndicator` during search suggestion and mood loading states.

### 📚 Library UX Improvements
- **Slide-Up Playlist Options**: Converted the Library plus (`+`) button dialog into an interactive `ModalBottomSheet` that smoothly slides up from the bottom with haptic feedback.

### 🌈 Theme & Palette Updates
- **Default Theme to Aurora**: Switched default app color palette to `"aurora"` across theme pickers, creators, and default preferences.
- **Pure Black Forced Off**: Pure Black AMOLED toggle defaulted to `false` and actively enforced off to ensure rich surface contrast.

### ⚙️ Engine & Audio Stability
- **Audio DSP Processor**: Enhanced floating-point calculations and boundary clamping in native C++ DSP pipeline.
- **Lossless Stream Resolver**: Improved resilience when resolving Apple Music ALAC and YouTube audio streams.
- **Version Bump**: Bumped `versionCode` to `48` and `versionName` to `2.22.37`.

---

## [v2.22.36] - 2026-10-06
- YouTube video canvas in Apple Music Player.
- Preserved Apple Music canvas experience with ambient visuals.
- Playback & search stall bug fixes.
- Navigation bar layout range coercion crash prevention.
