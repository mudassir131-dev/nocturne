"use strict";

/**
 * Nocturne backend entry point.
 *
 * Exposes:
 *   GET /                 -> health check
 *   GET /search?q=        -> YouTube search via yt-dlp
 *   GET /stream/:videoId  -> best audio piped to the client
 *   GET /download/:id     -> mp3 / mp4 file download
 *
 * Requires the `yt-dlp` binary to be on PATH (or YTDLP_BIN to point to it).
 */

const express = require("express");
const cors = require("cors");
const https = require("https");
const http = require("http");

const searchRoute = require("./routes/search");
const streamRoute = require("./routes/stream");
const downloadRoute = require("./routes/download");

const app = express();

app.use(cors());
app.use(express.json());

app.get("/", (_req, res) => {
  res.type("text/plain").send("Server running");
});

app.get("/healthz", (_req, res) => {
  res.json({ ok: true, ts: Date.now() });
});

app.use("/search", searchRoute);
app.use("/stream", streamRoute);
app.use("/download", downloadRoute);

app.use((req, res) => {
  res.status(404).json({ error: "Not found", path: req.path });
});

const PORT = parseInt(process.env.PORT || "3000", 10);

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`[nocturne-backend] listening on :${PORT}`);
  });

  // Self-ping every 14 minutes so free-tier hosts (e.g. Railway, Render)
  // don't put the server to sleep. Set SELF_URL to your public URL.
  const selfUrl = process.env.SELF_URL;
  if (selfUrl) {
    setInterval(() => {
      try {
        const client = selfUrl.startsWith("https") ? https : http;
        client
          .get(selfUrl, (res) => {
            res.resume();
          })
          .on("error", (err) => {
            console.warn("[self-ping] failed:", err.message);
          });
      } catch (err) {
        console.warn("[self-ping] threw:", err);
      }
    }, 14 * 60 * 1000);
  }
}

module.exports = app;
