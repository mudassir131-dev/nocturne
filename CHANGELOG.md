# Nocturne Changelog & Release Notes

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
