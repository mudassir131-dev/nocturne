"use strict";

const express = require("express");
const { search } = require("../utils/ytdlp");

const router = express.Router();

/**
 * GET /search?q=query
 *
 * Returns: [{ id, title, artist, thumbnail, duration }]
 */
router.get("/", async (req, res) => {
  const q = (req.query.q || "").toString().trim();
  if (!q) {
    res.status(400).json({ error: "Missing query parameter 'q'" });
    return;
  }

  const limit = Math.min(
    parseInt((req.query.limit || "10").toString(), 10) || 10,
    25,
  );

  try {
    const results = await search(q, { limit });
    res.json(results);
  } catch (err) {
    console.error("[/search] yt-dlp error:", err);
    res.status(500).json({
      error: "Search failed",
      detail: err.message || String(err),
    });
  }
});

module.exports = router;
