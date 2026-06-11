# 🎵 Nocturne

<p align="center">
  <img src="https://img.shields.io/github/actions/workflow/status/mudassir131-dev/nocturne/build-apk.yml?branch=main&style=for-the-badge&logo=github&label=Build%20Status&color=4CAF50" alt="Build Status">
  <img src="https://img.shields.io/github/license/mudassir131-dev/nocturne?style=for-the-badge&logo=github&color=2196F3" alt="License">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform">
</p>

---

**Nocturne** is an elegant, open-source, ad-free YouTube Music client built natively for Android. Combining the speed of **Kotlin** with the modern interface design of **Jetpack Compose**, it offers a buttery-smooth, fluid, and premium music streaming experience.

Nocturne runs **fully standalone** on your device, connecting directly to streaming APIs without relying on external servers or heavy background processes.

---

## ✨ Key Features

*   🚫 **Ad-Free Streaming:** Enjoy uninterrupted music without any commercial interruptions.
*   🎨 **Dynamic Material You Theme:** The entire application interface adapts dynamically to the color palette of your current song's album art.
*   🎧 **Advanced Audio Control:** Features gapless playback, crossfade transitions, and audio normalization.
*   📜 **Real-Time Lyrics:** Built-in lyrics engine that fetches, synchronizes, and displays lyrics in real-time.
*   💾 **Smart Offline Caching:** Automatically caches recently played songs for fast offline playback.
*   🚀 **Highly Optimized Performance:** Fully native build with zero webviews or wrappers, offering excellent battery life and instant responsiveness.

---

## 📲 Installation

You can easily download and install the pre-compiled packages directly from this repository:

1.  Navigate to the **Actions** tab of this repository on GitHub.
2.  Select the latest successful run of the **"Build Android APK"** workflow.
3.  Scroll down to the **Artifacts** section at the bottom of the page.
4.  Download the package suitable for your device:
    *   **`app-universal-release`**: Compatible with all Android devices.
    *   **`app-arm64-release`**: Optimized for modern ARM64 mobile processors.
5.  Extract the downloaded zip file and install the `.apk` on your Android device!

---

## 🛠️ Project Architecture

The codebase is modularized to ensure readability, separation of concerns, and optimal build times:

| Module | Description |
| :--- | :--- |
| **`app/`** | The main application module containing the Jetpack Compose UI, ViewModels, and core application logic. |
| **`innertube/`** | A dedicated connection wrapper that interacts directly with streaming endpoints. |
| **`betterlyrics/`** | The engine responsible for fetching, parsing, and synchronizing lyrics. |
| **`canvas/`** | Visual elements and animations displayed during playback. |
| **`simpmusic/`** | Internal alternative lyrics utility. |
| **`kugou/`** / **`lrclib/`** / **`lastfm/`** | Custom API wrappers for lyrics search, metadata retrieval, and audio scrobbling. |

---

## 🏗️ Local Development

To compile Nocturne locally on your machine, follow these steps:

1.  Clone this repository to your local setup:
    ```bash
    git clone https://github.com/mudassir131-dev/nocturne.git
    ```
2.  Open the project directory in **Android Studio (Koala or later)**.
3.  Make sure you have **JDK 21** configured in your Gradle settings.
4.  Build the project directly, or run the following command in the terminal to compile:
    ```bash
    ./gradlew assembleUniversalRelease
    ```

---

## ⚖️ License

Nocturne is licensed under the **GNU General Public License v3.0**. See the `LICENSE` file for more details.
