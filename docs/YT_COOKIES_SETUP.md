# Setting up YouTube cookies for the Nocturne backend

YouTube blocks `yt-dlp` requests coming from data-center IPs (Railway, Fly,
Heroku, AWS, …) with the message:

> Sign in to confirm you're not a bot.

Search still works (it uses `--flat-playlist`, which doesn't trigger the
check), but per-video extraction — i.e. the `/stream/:videoId` and
`/download/:videoId` endpoints — fails until you provide cookies from a
logged-in YouTube session.

The backend reads cookies from one of two environment variables:

- `YTDLP_COOKIES_BASE64` — base64-encoded contents of a `cookies.txt` file.
  **Easiest** option on Railway because env vars are first-class there.
- `YTDLP_COOKIES_FILE` — absolute path to a `cookies.txt` file on disk.
  Useful if you mount a volume.

If neither is set, the backend logs a warning and continues anyway (so it
keeps booting); only `/stream` and `/download` are affected.

---

## Step 1 — Export cookies from your browser

1. Install the **"Get cookies.txt LOCALLY"** browser extension. It's
   open-source and does the export entirely in your browser (does **not**
   upload anywhere):
   - Chrome / Brave / Edge: <https://chromewebstore.google.com/detail/get-cookiestxt-locally/cclelndahbckbenkjhflpdbgdldlbecc>
   - Firefox: <https://addons.mozilla.org/en-US/firefox/addon/cookies-txt-one-click/>
2. Open <https://www.youtube.com> and **sign in** with your Google account.
3. Click the extension icon → **Export** → save as `cookies.txt`.

**Use a throwaway Google account if you can.** YouTube can flag accounts
whose cookies it sees being used from unusual IPs. A burner account avoids
risking your main login.

## Step 2 — Encode the file as base64

On macOS / Linux:

```bash
base64 -w0 cookies.txt > cookies.b64
# or, if -w0 isn't supported (BSD base64):
base64 cookies.txt | tr -d '\n' > cookies.b64
cat cookies.b64
```

On Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("cookies.txt"))
```

Copy the entire single-line output to your clipboard.

## Step 3 — Add the variable on Railway

1. Open your Nocturne project on <https://railway.app>.
2. Service → **Variables** tab → **+ New Variable**.
3. Name: `YTDLP_COOKIES_BASE64`. Value: paste the base64 string. **Save**.
4. Railway redeploys automatically. Wait ~1 minute.

The deployment logs should now contain:

```
[ytdlp] using cookies file: /tmp/yt-dlp-cookies.txt
```

Verify with:

```bash
curl -I https://<your-railway-url>/stream/dQw4w9WgXcQ
# Expect: HTTP/2 200, Content-Type: audio/mpeg, plus actual bytes streaming.
```

## Step 4 — When to refresh

YouTube cookies expire (typically every 30–60 days). If streaming starts
returning 502s again, repeat steps 1–3 with a fresh export.

---

## FAQ

**Q: Is it safe to upload my cookies to Railway?**
The cookies grant access to your YouTube account. Anyone with read access to
the env var (i.e. anyone who can administer the Railway project) can
impersonate your YouTube session. That's why we recommend a **throwaway
Google account**.

**Q: Why not extract YouTube directly from the Flutter app?**
The `youtube_explode_dart` package can do this, but YouTube's per-video
endpoints fingerprint the client's TLS handshake and IP. Mobile devices
generally don't get bot-checked, but the same package on a desktop/emulator
or behind some carriers does. Backed extraction with cookies is the most
reliable, especially when running on a public app store build.

**Q: Can I use a residential proxy instead of cookies?**
Yes — set `HTTPS_PROXY` / `HTTP_PROXY` on the Railway service to point at a
residential proxy, and yt-dlp will route through it. This is more expensive
but doesn't require sharing a login session.
