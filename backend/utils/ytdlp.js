"use strict";

const { spawn, execFile } = require("child_process");
const { promisify } = require("util");

const execFileAsync = promisify(execFile);

/**
 * Path to the `yt-dlp` binary. Override with the YTDLP_BIN env var if it
 * lives somewhere non-standard (e.g. inside a Railway image).
 */
const YTDLP_BIN = process.env.YTDLP_BIN || "yt-dlp";

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
  const args = [
    "-f",
    "bestaudio",
    "-o",
    "-",
    "--no-playlist",
    "--quiet",
    "--no-warnings",
    videoUrl(videoId),
  ];
  return spawn(YTDLP_BIN, args, { stdio: ["ignore", "pipe", "pipe"] });
}

/**
 * Run `yt-dlp` to download the given videoId to `outDir`.
 *
 * `type` is either "audio" (mp3) or "video" (best mp4).
 * Resolves with the absolute path of the produced file.
 */
async function download(videoId, { type = "audio", outDir }) {
  const baseArgs = [
    "--no-playlist",
    "--no-warnings",
    "-o",
    `${outDir}/%(id)s.%(ext)s`,
    "--print",
    "after_move:filepath",
    videoUrl(videoId),
  ];
  const args =
    type === "audio"
      ? ["-x", "--audio-format", "mp3", ...baseArgs]
      : ["-f", "bestvideo*+bestaudio/best", ...baseArgs];

  const { stdout } = await execFileAsync(YTDLP_BIN, args, {
    maxBuffer: 1024 * 1024 * 16,
    timeout: 5 * 60_000,
  });
  return stdout.split("\n").map((s) => s.trim()).filter(Boolean).pop();
}

module.exports = { search, streamAudio, download, videoUrl, YTDLP_BIN };
