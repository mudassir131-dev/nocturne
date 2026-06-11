"use strict";

const { spawn, execFile } = require("child_process");
const fs = require("fs");
const path = require("path");
const os = require("os");
const { promisify } = require("util");

const execFileAsync = promisify(execFile);

/**
 * Path to the `yt-dlp` binary. Override with the YTDLP_BIN env var.
 * Fallback to local bin directory, then to system path.
 */
const localBinFile = path.join(
  __dirname,
  "..",
  "bin",
  process.platform === "win32" ? "yt-dlp.exe" : "yt-dlp"
);
const YTDLP_BIN = process.env.YTDLP_BIN || (fs.existsSync(localBinFile) ? localBinFile : "yt-dlp");

let ffmpegDir = "";
try {
  const ffmpegPath = require("ffmpeg-static");
  if (ffmpegPath && fs.existsSync(ffmpegPath)) {
    ffmpegDir = path.dirname(ffmpegPath);
    console.log(`[ytdlp] Using ffmpeg-static path: ${ffmpegDir}`);
  }
} catch (e) {
  console.warn("[ytdlp] ffmpeg-static not found or failed to load. Will fallback to system PATH.");
}

function getSpawnOptions() {
  const env = { ...process.env };
  if (ffmpegDir) {
    env.PATH = `${ffmpegDir}${path.delimiter}${env.PATH || ""}`;
  }
  return { env };
}

/**
 * Resolved path to a cookies.txt file used by yt-dlp.
 *
 * YouTube blocks data-center IPs (Railway, Fly, Heroku, etc.) with a
 * "Sign in to confirm you're not a bot" check on per-video extraction.
 * Passing cookies from a logged-in YouTube session bypasses the check.
 *
 * Two ways to provide cookies:
 *   - YTDLP_COOKIES_FILE = absolute path to a cookies.txt file
 *   - YTDLP_COOKIES_BASE64 = base64 of the cookies.txt content
 *     (preferred on Railway since env vars are easy to set)
 */
const COOKIES_PATH = (() => {
  if (process.env.YTDLP_COOKIES_FILE) {
    if (fs.existsSync(process.env.YTDLP_COOKIES_FILE)) {
      return process.env.YTDLP_COOKIES_FILE;
    }
    console.warn(
      `[ytdlp] YTDLP_COOKIES_FILE=${process.env.YTDLP_COOKIES_FILE} not found`,
    );
  }
  if (process.env.YTDLP_COOKIES_BASE64) {
    try {
      const tmp = path.join(os.tmpdir(), "yt-dlp-cookies.txt");
      const txt = Buffer.from(
        process.env.YTDLP_COOKIES_BASE64,
        "base64",
      ).toString("utf8");
      fs.writeFileSync(tmp, txt, { mode: 0o600 });
      return tmp;
    } catch (e) {
      console.warn(`[ytdlp] failed to materialize YTDLP_COOKIES_BASE64: ${e}`);
    }
  }
  return null;
})();

if (COOKIES_PATH) {
  console.log(`[ytdlp] using cookies file: ${COOKIES_PATH}`);
} else {
  console.warn(
    "[ytdlp] no cookies configured — YouTube may block streaming from cloud IPs.",
  );
}

/**
 * Default extra args appended to every yt-dlp invocation that hits per-video
 * endpoints (stream / download — NOT search, since `--flat-playlist` skips
 * the bot-checked extraction path).
 *
 * `player_client=tv,web_safari` is the combination that currently has the
 * best success rate against YouTube's bot detection on data-center IPs as
 * of yt-dlp 2024-2025; falling back to `web` lets it use the standard
 * extractor when neither of the others returns a manifest.
 */
const EXTRACTOR_ARGS_VIDEO = [
  "--extractor-args",
  "youtube:player_client=default,-tv,web_safari,web_embedded",
];

function withCookies(args) {
  if (!COOKIES_PATH) return args;
  return ["--cookies", COOKIES_PATH, ...args];
}

/**
 * Build a YouTube watch URL for a given videoId.
 */
function videoUrl(videoId) {
  return `https://www.youtube.com/watch?v=${videoId}`;
}

/**
 * Run `yt-dlp ytsearchN:query --dump-json --flat-playlist` and return
 * a normalized array of search hits.
 *
 * `--flat-playlist` keeps it fast (no extraction of per-video metadata).
 */
async function search(query, { limit = 10 } = {}) {
  const args = [
    `ytsearch${limit}:${query}`,
    "--dump-json",
    "--flat-playlist",
    "--no-warnings",
  ];
  const { stdout } = await execFileAsync(YTDLP_BIN, args, {
    maxBuffer: 1024 * 1024 * 16,
    timeout: 60_000,
    ...getSpawnOptions(),
  });
  return stdout
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .map((line) => {
      try {
        return JSON.parse(line);
      } catch (_) {
        return null;
      }
    })
    .filter(Boolean)
    .map((entry) => ({
      id: entry.id,
      title: entry.title || "",
      artist: entry.uploader || entry.channel || "Unknown",
      thumbnail:
        entry.thumbnail ||
        (entry.id ? `https://i.ytimg.com/vi/${entry.id}/hqdefault.jpg` : ""),
      duration:
        typeof entry.duration === "number" ? Math.round(entry.duration) : null,
    }))
    .filter((h) => h.id);
}

/**
 * Spawn `yt-dlp -f bestaudio -o - <url>` and return the child process.
 *
 * The caller is responsible for piping stdout to the HTTP response and
 * cleaning up the process on disconnect.
 */
function streamAudio(videoId) {
  const args = withCookies([
    ...EXTRACTOR_ARGS_VIDEO,
    "-f",
    "bestaudio",
    "-o",
    "-",
    "--no-playlist",
    "--quiet",
    "--no-warnings",
    videoUrl(videoId),
  ]);
  return spawn(YTDLP_BIN, args, {
    stdio: ["ignore", "pipe", "pipe"],
    ...getSpawnOptions(),
  });
}

/**
 * Run `yt-dlp` to download the given videoId to `outDir`.
 *
 * `type` is either "audio" (mp3) or "video" (best mp4).
 * Resolves with the absolute path of the produced file.
 */
async function download(videoId, { type = "audio", outDir }) {
  const baseArgs = withCookies([
    ...EXTRACTOR_ARGS_VIDEO,
    "--no-playlist",
    "--no-warnings",
    "-o",
    `${outDir}/%(id)s.%(ext)s`,
    "--print",
    "after_move:filepath",
    videoUrl(videoId),
  ]);
  const args =
    type === "audio"
      ? ["-x", "--audio-format", "mp3", ...baseArgs]
      : ["-f", "bestvideo*+bestaudio/best", ...baseArgs];

  const { stdout } = await execFileAsync(YTDLP_BIN, args, {
    maxBuffer: 1024 * 1024 * 16,
    timeout: 5 * 60_000,
    ...getSpawnOptions(),
  });
  return stdout.split("\n").map((s) => s.trim()).filter(Boolean).pop();
}

module.exports = {
  search,
  streamAudio,
  download,
  videoUrl,
  YTDLP_BIN,
  COOKIES_PATH,
};
