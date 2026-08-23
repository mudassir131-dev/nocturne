<p align="center">
  <img src=".github/assets/app_logo.jpg" alt="Nocturne Logo" width="140" style="border-radius: 28px; box-shadow: 0 8px 24px rgba(0,0,0,0.35);" />
</p>

<h1 align="center">Nocturne</h1>

<p align="center">
  <strong>An immersive, elegant, and native open-source music experience for Android.</strong>
</p>

<p align="center">
  Stream music ad-free, follow word-by-word synchronized lyrics, enjoy seamless live backdrops with an Apple Music inspired player, and tailor your sound in pure native Kotlin & Jetpack Compose.
</p>

<p align="center">
  <a href="https://github.com/mudassir131-dev/nocturne/releases/latest"><img src="https://img.shields.io/github/v/release/mudassir131-dev/nocturne?display_name=release&style=for-the-badge&logo=github&label=Latest%20Release" alt="Latest release"></a>
  <a href="https://github.com/mudassir131-dev/nocturne/actions/workflows/build-apk.yml"><img src="https://img.shields.io/github/actions/workflow/status/mudassir131-dev/nocturne/build-apk.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=Android%20Build" alt="Android build"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0 or newer">
  <a href="LICENSE"><img src="https://img.shields.io/github/license/mudassir131-dev/nocturne?style=for-the-badge&color=7C4DFF" alt="License"></a>
</p>

<p align="center">
  <a href="https://github.com/mudassir131-dev/nocturne/releases/latest"><strong>⬇️ Download Nocturne</strong></a>
  &nbsp;•&nbsp;
  <a href="https://github.com/mudassir131-dev/nocturne/issues">🐛 Report an Issue</a>
  &nbsp;•&nbsp;
  <a href="https://github.com/mudassir131-dev/nocturne/discussions">💬 Community</a>
</p>

---

## 📸 Preview & Screenshots

<p align="center">
  <img src=".github/assets/screen_home.jpg" width="23%" alt="Home Screen" style="border-radius: 12px; margin: 4px;" />
  <img src=".github/assets/screen_search.jpg" width="23%" alt="Search & Exploration" style="border-radius: 12px; margin: 4px;" />
  <img src=".github/assets/screen_player.jpg" width="23%" alt="Apple Music Player Style" style="border-radius: 12px; margin: 4px;" />
  <img src=".github/assets/screen_lyrics.jpg" width="23%" alt="Real-time Synchronized Lyrics" style="border-radius: 12px; margin: 4px;" />
</p>

---

## ✨ Features

### 🎧 Pure Audio Playback & Streaming
- **Native Android Performance**: Built completely on Kotlin, Jetpack Compose, Material 3, and AndroidX Media3 / ExoPlayer.
- **YouTube Music & YouTube Integration**: Stream songs, music videos, artist profiles, and full albums directly.
- **Lossless & High Quality Streams**: Choose from various codec formats (OPUS, AAC, FLAC) with real-time bitrate & codec badges.
- **Offline Caching & Downloads**: Cache audio automatically during playback or download tracks directly for offline listening.

### 🎨 Apple Music-Inspired Player & Visual Engine
- **Immersive Full-Bleed Player**: Seamless artwork backdrops with smooth blur gradients and dynamic color extraction.
- **Nocturne Live Canvas**: Dynamic video and animated artwork backdrops that bring your playback screen to life.
- **Floating Island Navigation**: Modern, pill-shaped responsive navigation bar that adapts to your screen interactions.
- **Liquid Glassmorphism**: Optional translucent glass effects with customizable blur and specular reflections.

### 📜 Real-Time Synchronized Lyrics
- **Word-by-Word Timing**: Syllable-accurate karaoke synchronization and smooth scrolling animations.
- **Multi-Provider Fallback**: Instant lyrics fetching powered by BetterLyrics, LRCLIB, KuGou, and YouTube with customizable provider priority.
- **Interactive Lyric Seeking**: Tap any lyric line to jump directly to that exact moment in the song.
- **Romanization**: Automatic romanization support for Japanese and other non-Latin scripts.

### 📚 Personal Library & Discovery
- **Smart Recommendations**: Forgotten favorites, quick picks, keep listening carousels, and mood/genre playlists.
- **Complete Library Tools**: Organize favorite songs, custom tagged playlists, artist collections, and listening history.
- **Listening Statistics**: Track your most played songs, artists, and albums over customizable timeframes.
- **Import & Sync**: Easily import and sync playlists directly from YouTube and local backups.

### 🔗 Integrations & Ecosystem
- **Discord Rich Presence**: Share what you are listening to in real-time on Discord with rich playback badges.
- **ListenBrainz & Last.fm**: Native scrobbling support to keep your musical profile updated.
- **Android System Integration**: Full media notifications, lock-screen controls, Bluetooth metadata, Glance widget, and Android Auto support.
- **Music Together**: Listen with friends synchronously with real-time room sessions.

---

## 🌍 Supported Languages

Nocturne is fully localized and available in multiple languages thanks to our global contributors:

| Language | Locale | Language | Locale |
| :--- | :--- | :--- | :--- |
| **English** | `en` | **Hindi (हिन्दी)** | `hi-rIN` |
| **Spanish (Español)** | `es` / `es-rES` | **French (Français)** | `fr` / `fr-rFR` |
| **German (Deutsch)** | `de-rDE` | **Russian (Русский)** | `ru` / `ru-rRU` |
| **Japanese (日本語)** | `ja` | **Korean (한국어)** | `ko` |
| **Chinese Simplified (简体中文)** | `zh-rCN` | **Portuguese (Português)** | `pt-rBR` |
| **Italian (Italiano)** | `it` | **Turkish (Türkçe)** | `tr` |
| **Indonesian (Bahasa Indonesia)** | `in-rID` | **Vietnamese (Tiếng Việt)** | `vi` |
| **Arabic (العربية)** | `ar` | **Hebrew (עברית)** | `iw` |
| **Dutch (Nederlands)** | `nl` | **Malay (Bahasa Melayu)** | `ms` |
| **Malayalam (മലയാളം)** | `ml` | **Estonian (Eesti)** | `et` |

---

## 📥 Download & Installation

You can get the latest stable build of Nocturne from the [Releases](https://github.com/mudassir131-dev/nocturne/releases/latest) page.

| Architecture | Description | Download |
| :--- | :--- | :--- |
| **Universal APK** | Compatible with all supported devices (arm64, armeabi-v7a, x86_64) | [Download Universal APK](https://github.com/mudassir131-dev/nocturne/releases/latest) |
| **ARM64-v8a APK** | Optimized package with smaller size for modern 64-bit Android devices | [Download ARM64 APK](https://github.com/mudassir131-dev/nocturne/releases/latest) |

> **Requirement**: Android 8.0 (Oreo) or higher.

---

## 🛠️ Building from Source

To compile Nocturne from source:

1. **Clone the repository**:
   ```bash
   git clone https://github.com/mudassir131-dev/nocturne.git
   cd nocturne
   ```

2. **Open with Android Studio** (Ladybug / Jellyfish or newer recommended) or compile via command line:
   ```bash
   # Build Universal Debug APK
   ./gradlew assembleUniversalDebug

   # Run Unit Test Suite
   ./gradlew testUniversalDebugUnitTest
   ```

3. Output APK will be generated at:
   `app/build/outputs/apk/universal/debug/app-universal-debug.apk`

---

## 💖 Support the Developer

If you love using Nocturne and want to support its ongoing development, improvements, and server maintenance, you can donate directly via UPI:

<p align="center">
  <strong>UPI ID:</strong> <code>touseeparay7-1@okicici</code>
</p>

---

## 📄 License & Disclaimer

Nocturne is released under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for complete details.

### Disclaimer
*Nocturne is an independent, open-source project and is not affiliated with, authorized, maintained, or endorsed by Google LLC, YouTube, Apple Inc., Spotify, or any of their affiliates.*
