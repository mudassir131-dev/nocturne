"use strict";

const express = require("express");
const { streamAudio } = require("../utils/ytdlp");

const router = express.Router();

/**
 * GET /stream/:videoId
 *
 * Pipes the best audio track for the given YouTube videoId to the
 * client. Used by just_audio in the Flutter app.
 */
router.get("/:videoId", (req, res) => {
  const videoId = (req.params.videoId || "").toString();
  if (!/^[A-Za-z0-9_-]{6,20}$/.test(videoId)) {
    res.status(400).json({ error: "Invalid videoId" });
    return;
  }

  res.set({
    "Content-Type": "audio/mpeg",
    "Cache-Control": "no-store",
    "Accept-Ranges": "none",
  });

  const child = streamAudio(videoId);
  let closed = false;

  const cleanup = () => {
    if (closed) return;
    closed = true;
    try {
      child.kill("SIGKILL");
    } catch (_) {
      // ignore
    }
  };

  child.stdout.on("error", (err) => {
    console.error(`[/stream] stdout error for ${videoId}:`, err);
    cleanup();
    if (!res.headersSent) {
      res.status(500).end();
    } else {
      res.end();
    }
  });

  child.stderr.on("data", (chunk) => {
    process.stderr.write(`[yt-dlp:${videoId}] ${chunk}`);
  });

  child.on("error", (err) => {
    console.error(`[/stream] spawn error for ${videoId}:`, err);
    cleanup();
    if (!res.headersSent) {
      res.status(500).json({ error: "yt-dlp failed to start" });
    }
  });

  child.on("exit", (code) => {
    if (code !== 0 && !res.headersSent) {
      res
        .status(502)
        .json({ error: `yt-dlp exited with code ${code}` });
    }
    cleanup();
  });

  req.on("close", cleanup);
  req.on("aborted", cleanup);

  child.stdout.pipe(res);
});

module.exports = router;
