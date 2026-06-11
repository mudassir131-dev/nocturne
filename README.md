# Nocturne

An open-source, ad-free YouTube Music streaming client for Android. 

This project is now a native Android application built using **Kotlin** and **Jetpack Compose**, providing a highly responsive, modern, and buttery-smooth experience.

*Inspired by and built upon the excellent work of [nikhilvishwakarma00/Velune](https://github.com/nikhilvishwakarma00/Velune).*

---

## Features

- **Ad-Free Playback:** Uninterrupted background music streaming.
- **Dynamic Theme:** Material You dynamic color system that adapts to your playing album art.
- **Audio Control:** Gapless playback, crossfade, and lyrics.
- **Offline Caching:** Fast caching of played songs for offline access.
- **Automated Builds:** Every commit built automatically via GitHub Actions.

---

## Installation

You can download the compiled installer package (**APK**) directly from the repository:

1. Go to the **Actions** tab of this repository on GitHub.
2. Click on the latest workflow run (e.g., "Build Android APK").
3. Scroll down to the **Artifacts** section at the bottom of the page.
4. Download **`app-universal-debug`** (works on all Android devices) or **`app-arm64-debug`** (optimized for ARM64 devices).
5. Extract the downloaded zip file and install the `.apk` on your phone!

---

## Project Structure

- **`app/`** — Native Android Kotlin/Compose app UI and business logic.
- **`innertube/`** — Direct client-side connection wrapper for YouTube Music APIs.
- **`betterlyrics/`** — Lyrics syncing and parsing.
- **`backend/`** — Node.js Express server (retained for reference; the Android client runs fully standalone and does not require this backend).

---

## Credits

- **Original Author & Inspiration:** [nikhilvishwakarma00](https://github.com/nikhilvishwakarma00) for [Velune](https://github.com/nikhilvishwakarma00/Velune).
- **Owner & Maintainer:** [mudassir131-dev](https://github.com/mudassir131-dev).

---

## License

GPL-3.0-only
