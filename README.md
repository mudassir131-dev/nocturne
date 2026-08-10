# Nocturne

<p align="center">
  <strong>A premium, open-source music experience built natively for Android.</strong>
</p>

<p align="center">
  Discover music, build your library, follow synchronized lyrics, and enjoy an immersive player designed around motion, artwork, and sound.
</p>

<p align="center">
  <a href="https://github.com/mudassir131-dev/nocturne/releases/latest"><img src="https://img.shields.io/github/v/release/mudassir131-dev/nocturne?display_name=release&style=for-the-badge&logo=github&label=Latest%20Release" alt="Latest release"></a>
  <a href="https://github.com/mudassir131-dev/nocturne/actions/workflows/build-apk.yml"><img src="https://img.shields.io/github/actions/workflow/status/mudassir131-dev/nocturne/build-apk.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=Android%20Build" alt="Android build"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0 or newer">
  <a href="LICENSE"><img src="https://img.shields.io/github/license/mudassir131-dev/nocturne?style=for-the-badge&color=7C4DFF" alt="License"></a>
</p>

<p align="center">
  <a href="https://github.com/mudassir131-dev/nocturne/releases/latest"><strong>Download Nocturne</strong></a>
  ·
  <a href="https://github.com/mudassir131-dev/nocturne/issues">Report a bug</a>
  ·
  <a href="https://github.com/mudassir131-dev/nocturne/actions">Build status</a>
</p>

---

## Overview

Nocturne is an unofficial, community-built Android music client focused on a polished listening experience. It combines a native Kotlin and Jetpack Compose interface with Media3 playback, dynamic artwork-driven themes, configurable lyrics providers, library tools, listening history, statistics, and deep customization.

The project is developed by **M Labs / Mudassir** and distributed as open-source software. Nocturne is not affiliated with, sponsored by, or endorsed by YouTube, YouTube Music, Google, Apple Music, Spotify, Discord, Last.fm, or any other third-party service referenced by the application.

> [!IMPORTANT]
> Availability of music, metadata, lyrics, canvas media, and integrations depends on network access and third-party services. Those services can change independently of Nocturne.

## Contents

- [Why Nocturne](#why-nocturne)
- [Features](#features)
- [Lyrics engine](#lyrics-engine)
- [Player and visual system](#player-and-visual-system)
- [Download and install](#download-and-install)
- [Updating](#updating)
- [Permissions](#permissions)
- [Build from source](#build-from-source)
- [Release signing](#release-signing)
- [Architecture](#architecture)
- [Privacy and security](#privacy-and-security)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Why Nocturne

- **Native Android experience** — Kotlin, Jetpack Compose, Material 3, and AndroidX Media3.
- **Premium visual direction** — dynamic color, immersive artwork, animated surfaces, contextual capsules, and optional glass effects.
- **Flexible player** — the standard Nocturne presentation and an optional Apple-inspired full-screen player.
- **Provider-based lyrics** — six selectable sources with persistent priority ordering and automatic fallback.
- **Personal listening space** — favorites, playlists, downloads, cache, history, statistics, and quick picks.
- **System media integration** — notifications, lock screen, headset, Bluetooth, widget, and Android Auto metadata.
- **No WebView shell** — the main interface and playback stack use native Android components.

## Features

### Discovery and search

- Search songs, videos, albums, artists, and playlists.
- Explore mood and genre collections.
- Browse suggestions and album-focused results.
- Curated home sections, quick picks, keep-listening shelves, and personalized discovery.
- Open supported YouTube and YouTube Music links directly in Nocturne.
- Receive supported links through Android’s share sheet.

### Library and history

- Like songs and manage playlists.
- Browse songs, albums, artists, downloads, cached media, and personal top tracks.
- Switch between supported library layouts and sorting modes.
- Maintain local playback history.
- Review listening statistics across selectable time ranges.
- Resume playback through the persistent MiniPlayer.

### Playback

- Media3/ExoPlayer-based audio playback.
- Background playback through an Android media-session service.
- Lock-screen, notification, Bluetooth, headset, and system media controls.
- Queue management, shuffle, repeat, play next, and continue-playing behavior.
- Seek, volume, audio-output, and sleep-timer controls.
- Download and cache support for supported media.
- Home-screen widget and Android automotive media metadata.

### Personalization

- Light, dark, and artwork-driven dynamic themes.
- Multiple app icons: Eclipse, Midnight, Aura, and Pulse.
- Configurable player backgrounds, button colors, sliders, artwork treatment, and gestures.
- Optional floating navigation with scroll-responsive behavior.
- Optional Liquid Glass effects and per-component glass controls.
- Configurable lyrics text size, spacing, alignment, and animations.
- Personalized local onboarding.

### Imports and integrations

- Supported Spotify and Apple Music playlist-import workflows.
- Last.fm scrobbling when configured.
- Optional Discord Rich Presence.
- Shareable song cards and Android sharing support.
- Nocturne playback deep links and supported music URL handling.

> [!CAUTION]
> Discord Rich Presence uses a third-party Discord Gateway implementation. Review the in-app warning before enabling it and use it at your own discretion.

## Lyrics engine

Nocturne uses a provider resolver rather than relying on one lyrics service. Providers can be enabled independently, and their priority can be rearranged under **Settings → Content → Lyrics**.

| Provider | Purpose |
| --- | --- |
| **LrcLib** | General plain and synchronized lyrics lookup. |
| **KuGou** | Alternative catalog and synchronized lyrics source. |
| **BetterLyrics** | Enhanced synchronized and word-aware data where available. |
| **SimpMusic** | Alternative synchronized lyrics source. |
| **YouLyPlus** | Multi-server community lyrics lookup. |
| **PaxSenix** | Detailed synchronized lyrics and finer timing when provided upstream. |

The resolver follows the saved priority and moves to the next enabled provider when a source returns no usable result. Provider availability, synchronization quality, translations, and romanization vary by track and upstream service.

Additional lyrics capabilities include:

- Line-synchronized and word-aware presentation where timing exists.
- Plain lyrics fallback.
- Persistent provider priority.
- Optional queue preloading.
- Japanese and Korean romanization options.
- Search, refetch, edit, and translation actions where supported.

## Player and visual system

### Nocturne player

The default player is a flexible Compose surface with configurable artwork, background effects, controls, colors, sliders, gestures, queue access, and lyrics access.

### Apple-inspired player

The optional Apple-inspired presentation includes:

- Full-screen artwork-led composition.
- Adaptive colors derived from current artwork.
- Large transport controls and a simplified hierarchy.
- Quick access to queue, audio controls, sleep timer, and lyrics.
- Optional high-resolution and animated artwork behavior, subject to source availability and data-saving preferences.

### Dynamic visual layers

Nocturne can use active media artwork to influence the surrounding interface. Depending on device support and enabled settings, this can include dynamic palettes, blurred artwork fields, animated canvas media, translucent surfaces, and Liquid Glass-style effects.

Performance-heavy effects are optional. Disable blur, canvas, or glass effects if a device shows increased battery usage, heat, or dropped frames.

## Download and install

Download signed production builds from the official [GitHub Releases](https://github.com/mudassir131-dev/nocturne/releases/latest) page.

| APK | Recommended for |
| --- | --- |
| `Nocturne-V2.22.21-arm64-v8a-release.apk` | Most modern Android phones and tablets. Smaller download. |
| `Nocturne-V2.22.21-universal-release.apk` | Unknown CPU architecture or broader compatibility. |

Current package ID: `com.mudassir131.nocturne`

### Installation

1. Download the required APK.
2. Allow installation from the browser or file manager when Android asks.
3. Open the APK and select **Install**.
4. Launch Nocturne and complete onboarding.

### Integrity verification

Production releases include `SHA256SUMS.txt`. Compare the downloaded APK against that file when integrity verification is required.

```powershell
Get-FileHash .\Nocturne-V2.22.21-universal-release.apk -Algorithm SHA256
```

```bash
sha256sum Nocturne-V2.22.21-universal-release.apk
```

> [!NOTE]
> Nocturne requires Android 8.0 (API 26) or newer and currently targets Android API 36.

## Updating

Install a newer official APK over the existing app to retain compatible data. Android only permits an in-place update when the package name and signing certificate match.

- Prefer production APKs from this repository.
- Do not install a debug APK over a production build.
- Do not uninstall first unless you intend to erase local app data.
- Back up important data before major upgrades when export tools are available.

A package-conflict message usually means the installed build uses a different application ID or signing certificate.

## Permissions

| Permission group | Purpose |
| --- | --- |
| Internet and network state | Search, streaming, artwork, metadata, lyrics, imports, and integrations. |
| Notifications | Media playback notification on supported Android versions. |
| Foreground service and wake lock | Background playback, downloads, and media controls. |
| Media audio / storage | Supported local audio and files on applicable Android versions. |
| Bluetooth connect | Bluetooth device and media-control behavior. |
| Boot completed | Restore supported service state after reboot. |
| Modify audio settings | Audio output and compatible audio-effect controls. |

Nocturne also declares supported link handlers and share targets so compatible URLs can open in the app.

## Build from source

### Requirements

- Git
- Android Studio with Android SDK 36, or an equivalent command-line SDK
- JDK 21
- Internet access for Gradle dependencies
- Windows, macOS, or Linux

### Clone and build

```bash
git clone https://github.com/mudassir131-dev/nocturne.git
cd nocturne
./gradlew :app:assembleUniversalDebug
```

Windows PowerShell:

```powershell
git clone https://github.com/mudassir131-dev/nocturne.git
Set-Location nocturne
.\gradlew.bat :app:assembleUniversalDebug
```

The output is below `app/build/outputs/apk/universal/debug/`. Debug builds use the `.debug` application ID suffix and can remain separate from production.

For a smaller ARM64 build:

```bash
./gradlew :app:assembleArm64Debug
```

### Optional developer credentials

Add only credentials needed for the feature being tested to the untracked `local.properties`:

```properties
LASTFM_API_KEY=your_api_key
LASTFM_SECRET=your_api_secret
TOGETHER_BEARER_TOKEN=your_optional_token
```

Never commit tokens, passwords, keystores, or signing material.

## Release signing

Release variants require an existing Android signing key. Local builds read:

```text
NOCTURNE_KEYSTORE_FILE
NOCTURNE_KEYSTORE_PASSWORD
NOCTURNE_KEY_ALIAS
NOCTURNE_KEY_PASSWORD
```

```bash
./gradlew --no-daemon :app:assembleUniversalRelease :app:assembleArm64Release
```

GitHub Actions uses encrypted repository secrets:

```text
NOCTURNE_RELEASE_KEYSTORE_BASE64
NOCTURNE_RELEASE_KEYSTORE_PASSWORD
NOCTURNE_RELEASE_KEY_ALIAS
NOCTURNE_RELEASE_KEY_PASSWORD
```

On a version tag, the workflow builds, verifies, signs, hashes, uploads, and publishes the production APKs. Never replace a production signing key after users install a release; Android requires the same certificate for future updates.

## Architecture

| Module | Responsibility |
| --- | --- |
| `app` | Android app, Compose UI, navigation, playback, database, preferences, and orchestration. |
| `innertube` | Browse, search, metadata, and stream-facing integration. |
| `lrclib` | LrcLib client and models. |
| `kugou` | KuGou client and parsing. |
| `betterlyrics` | BetterLyrics integration and enhanced timing data. |
| `simpmusic` | SimpMusic lyrics integration. |
| `youlyplus` | Multi-server YouLyPlus/LyricsPlus integration. |
| `paxsenixlyrics` | PaxSenix integration and detailed timing models. |
| `unison` | Shared lyrics normalization and resolver-facing models. |
| `canvas` | Canvas and motion-art integration. |
| `lastfm` | Last.fm models and network integration. |
| `kizzy` | Discord Rich Presence support. |

### Core stack

- Kotlin and Kotlin Coroutines
- Jetpack Compose and Material 3
- AndroidX Media3 / ExoPlayer
- Hilt dependency injection
- Room and DataStore
- WorkManager
- Ktor and OkHttp
- Coil
- Gradle Kotlin DSL

## Privacy and security

Nocturne does not require an M Labs account for core playback. Settings and local library state are stored on the device. Features that connect to external services send the information necessary to complete the requested operation.

This can include:

- Search, streaming, artwork, and metadata requests.
- Lyrics queries to enabled providers.
- Playlist import requests.
- Last.fm scrobbling when configured.
- Discord Rich Presence when enabled.
- Update checks and downloads through GitHub Releases.

Third-party services operate under their own terms and privacy policies. Never include private credentials in issues, screenshots, logs, or public discussions.

For security reports, contact the maintainer privately through the available GitHub contact channel. Do not create a public issue containing exploitable details, tokens, signing material, or personal data.

## Troubleshooting

<details>
<summary><strong>The app closes immediately after launch</strong></summary>

Install the latest production release. If it continues, collect `adb logcat`, include the device model and Android version, and open an issue. Clearing app data can recover corrupted preferences but removes local state.

</details>

<details>
<summary><strong>Android refuses to install an update</strong></summary>

Confirm the installed app and APK use the same production signing certificate. A debug build, fork, or differently signed APK cannot update the official package in place.

</details>

<details>
<summary><strong>Lyrics are unavailable or unsynchronized</strong></summary>

Enable multiple providers, review their priority, and use refetch. Not every provider has every song, and timing quality depends on upstream data.

</details>

<details>
<summary><strong>Playback or search suddenly stops working</strong></summary>

Confirm network access, try another track, and install the newest release. Upstream services can change without notice.

</details>

<details>
<summary><strong>Scrolling or visual effects feel slow</strong></summary>

Disable Liquid Glass, blur, animated canvas, or other performance-heavy options. Battery saver and thermal throttling can also reduce smoothness.

</details>

## Contributing

Contributions improving stability, accessibility, performance, translations, documentation, provider resilience, and Android compatibility are welcome.

1. Fork the repository.
2. Create a focused branch from `main`.
3. Keep changes scoped and avoid unrelated redesigns.
4. Build the relevant debug variant.
5. Test affected flows on a device or emulator when possible.
6. Do not commit secrets, signing files, APKs, or user data.
7. Open a pull request describing the problem, implementation, and verification.

Useful bug reports include the Nocturne version, APK architecture, Android version, device model, reproduction steps, expected and actual behavior, visual evidence when relevant, and sanitized logs.

## License

Nocturne is licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE).

This software is provided without warranty. Users are responsible for complying with applicable laws and third-party service terms. Product names, service names, trademarks, and artwork belong to their respective owners.

---

<p align="center">
  Built with Kotlin, Compose, and an unreasonable amount of attention to the player.
</p>

<p align="center">
  <strong>M Labs · Nocturne</strong>
</p>
