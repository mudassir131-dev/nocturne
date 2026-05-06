# Nocturne

A YouTube music streaming app inspired by Apple Music.

> Pure-black UI, red accents, liquid-glass dock & search bar, background
> audio with lock-screen controls, dynamic player background sampled
> from the album art, Firebase-synced liked songs and playlists.

## Stack

- **Frontend** — Flutter (Dart), Riverpod, just_audio + audio_service.
- **Backend** — Node.js (Express) shelling out to
  [`yt-dlp`](https://github.com/yt-dlp/yt-dlp) for search + audio.
- **Database** — Cloud Firestore (per-user library) +
  [Hive](https://pub.dev/packages/hive) for offline cache.
- **Auth** — Firebase Auth + Google Sign In.

## Repository layout

```
.
├── lib/                      # Flutter app
│   ├── main.dart
│   ├── screens/              # home, search, library, player, album, liked, profile
│   ├── widgets/              # liquid_glass_*, mini_player, song_tile, album_card
│   ├── services/             # api_service, audio_service, database_service
│   ├── state/                # player_provider (Riverpod)
│   ├── models/               # Song
│   └── utils/                # theme, config
├── backend/                  # Node.js + yt-dlp
│   ├── index.js
│   ├── routes/               # search, stream, download
│   └── utils/ytdlp.js
├── docs/
│   ├── ANDROID_SETUP.md
│   └── FIREBASE_SETUP.md
├── pubspec.yaml
└── README.md
```

## Quick start

### 1. Backend

```bash
cd backend
npm install
yt-dlp --version          # must be on PATH
node index.js             # listens on :3000
```

See [`backend/README.md`](backend/README.md) for Railway deployment.

### 2. Flutter app

The repo ships only Dart code; generate the native folders once:

```bash
flutter create --platforms=android,ios .
flutter pub get
```

Then add the Android manifest entries described in
[`docs/ANDROID_SETUP.md`](docs/ANDROID_SETUP.md) (audio_service + Firebase).

Run it pointing at your backend:

```bash
# Android emulator -> host machine
flutter run --dart-define=NOCTURNE_BACKEND_URL=http://10.0.2.2:3000

# Real device or production
flutter run --dart-define=NOCTURNE_BACKEND_URL=https://nocturne-backend.up.railway.app
```

### 3. Firebase (optional but recommended)

Follow [`docs/FIREBASE_SETUP.md`](docs/FIREBASE_SETUP.md) to:

1. Create a Firebase project.
2. Drop `google-services.json` into `android/app/`.
3. Run `flutterfire configure` to generate `lib/firebase_options.dart`.
4. Add `Firebase.initializeApp(...)` to `main.dart`.

The app runs with Firebase disabled — Firebase-backed features (cross-
device sync, Google Sign In) will silently fall back to the local Hive
cache until you configure it.

## Theme

| Token            | Value            |
| ---------------- | ---------------- |
| Background       | `#000000`        |
| Card             | `#1A1A1A`        |
| Accent           | `#E53935`        |
| Text primary     | `#FFFFFF`        |
| Text secondary   | `#888888`        |
| Card radius      | `16`             |
| Dock radius      | `35`             |
| Searchbar radius | `25`             |
| Player art radius| `24`             |
| Glass blur       | `sigmaX=25, sigmaY=25` |

Centralised in [`lib/utils/theme.dart`](lib/utils/theme.dart).

## Configuration

The single source of truth for the backend URL is
[`lib/utils/config.dart`](lib/utils/config.dart). Override it at build
time via `--dart-define=NOCTURNE_BACKEND_URL=...` so you never have to
edit code to switch between local and production backends.

## Hosting

- **Backend** — Railway.app (free tier).
  See [`backend/Dockerfile`](backend/Dockerfile) and
  [`backend/railway.json`](backend/railway.json).
- **Database** — Firebase Spark plan.
- **Keep alive** — UptimeRobot pinging `/` every 5 minutes;
  the server also self-pings every 14 minutes when `SELF_URL` is set.

## License

MIT.
