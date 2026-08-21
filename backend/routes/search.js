"use strict";

const express = require("express");
const { search } = require("../utils/ytdlp");
const { searchCache } = require("../utils/cache");

const router = express.Router();

// Cache TTL: 15 minutes
const SEARCH_CACHE_TTL_MS = 15 * 60 * 1000;

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

  const cacheKey = `${q}:${limit}`;

  try {
    // Check cache
    const cachedResults = searchCache.get(cacheKey);
    if (cachedResults) {
      console.log(`[search] Cache hit for query: "${q}" (limit: ${limit})`);
      res.json(cachedResults);
      return;
    }

    console.log(`[search] Cache miss. Searching YouTube for: "${q}" (limit: ${limit})`);
    const results = await search(q, { limit });
    
    // Save to cache
    searchCache.set(cacheKey, results, SEARCH_CACHE_TTL_MS);
    
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
