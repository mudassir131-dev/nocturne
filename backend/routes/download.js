"use strict";

const express = require("express");
const fs = require("fs");
const path = require("path");
const { download } = require("../utils/ytdlp");

const router = express.Router();

const DOWNLOAD_DIR = path.resolve(__dirname, "..", "downloads");
fs.mkdirSync(DOWNLOAD_DIR, { recursive: true });

/**
 * GET /download/:videoId?type=audio|video
 *
 * Downloads the requested video via yt-dlp into the local downloads
 * directory and streams the resulting file back to the client.
 */
router.get("/:videoId", async (req, res) => {
  const videoId = (req.params.videoId || "").toString();
  const type = (req.query.type || "audio").toString();

  if (!/^[A-Za-z0-9_-]{6,20}$/.test(videoId)) {
    res.status(400).json({ error: "Invalid videoId" });
    return;
  }
  if (type !== "audio" && type !== "video") {
    res.status(400).json({ error: "type must be 'audio' or 'video'" });
    return;
  }

  try {
    const filePath = await download(videoId, { type, outDir: DOWNLOAD_DIR });
    if (!filePath || !fs.existsSync(filePath)) {
      res
        .status(500)
        .json({ error: "Download produced no output file" });
      return;
    }
    res.download(filePath, path.basename(filePath), (err) => {
      if (err) {
        console.error(`[/download] send error for ${videoId}:`, err);
      }
    });
  } catch (err) {
    console.error(`[/download] yt-dlp error for ${videoId}:`, err);
    res.status(500).json({
      error: "Download failed",
      detail: err.message || String(err),
    });
  }
});

module.exports = router;
