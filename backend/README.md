# Nocturne Backend

Express server that exposes YouTube search, audio streaming, and file
downloads to the Nocturne Flutter app. All heavy lifting is done by
[`yt-dlp`](https://github.com/yt-dlp/yt-dlp).

## Routes

| Method | Path                  | Description                                |
| ------ | --------------------- | ------------------------------------------ |
| GET    | `/`                   | Health check, returns `Server running`.    |
| GET    | `/search?q=...`       | Top 10 YouTube hits for the query.         |
| GET    | `/stream/:videoId`    | Best-audio MP3-ish stream (Content-Type: `audio/mpeg`). |
| GET    | `/download/:videoId?type=audio\|video` | File download (`audio` -> mp3, `video` -> mp4). |

## Local development

Requires Node 18+ and `yt-dlp` on `PATH` (and `ffmpeg` for mp3 conversion).

```bash
cd backend
npm install
yt-dlp --version  # confirm the binary works
PORT=3000 node index.js
```

Then point the Flutter app at it via:

```bash
flutter run --dart-define=NOCTURNE_BACKEND_URL=http://10.0.2.2:3000
```

(`10.0.2.2` is the Android emulator's alias for the host's `localhost`.)

## Deploying to Railway

1. Create a new Railway project pointing at this `backend/` directory.
2. Railway will build the included `Dockerfile`, which installs
   `yt-dlp` + `ffmpeg`.
3. Set environment variables:
   - `PORT` — Railway provides this automatically.
   - `SELF_URL` — your public URL (e.g.
     `https://nocturne-backend.up.railway.app`). When set, the server
     pings itself every 14 minutes to avoid sleep.
4. Add an UptimeRobot monitor hitting `/` every 5 minutes for extra
   safety.

## Environment variables

| Name        | Default      | Description                                    |
| ----------- | ------------ | ---------------------------------------------- |
| `PORT`      | `3000`       | HTTP port to listen on.                        |
| `YTDLP_BIN` | `yt-dlp`     | Path to the `yt-dlp` binary.                   |
| `SELF_URL`  | _(unset)_    | Public URL for the self-ping keep-alive.       |

## Notes

- `/stream` keeps `Accept-Ranges: none` because yt-dlp transcodes on
  the fly; just_audio in the Flutter app handles buffering.
- `/download` writes files into `backend/downloads/` (gitignored) and
  serves them via `res.download`.
- All routes are CORS-open so the Flutter client can reach them from
  any origin.
