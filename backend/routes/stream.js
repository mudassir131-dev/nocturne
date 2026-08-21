"use strict";

const express = require("express");
const https = require("https");
const { getVideoInfo } = require("../utils/ytdlp");
const { streamCache } = require("../utils/cache");

const router = express.Router();

/**
 * Pipes audio from a Google Video direct stream URL to the client.
 * Supports HTTP Range requests and redirect handling.
 */
function streamFromUrl(streamUrl, headers, req, res, onExpired) {
  let closed = false;
  let targetReq = null;

  const makeRequest = (url, redirectCount = 0) => {
    if (redirectCount > 3) {
      if (!res.headersSent) {
        res.status(502).json({ error: "Too many redirects" });
      }
      return;
    }

    try {
      const parsedUrl = new URL(url);
      const options = {
        method: "GET",
        headers: {
          ...headers,
          host: parsedUrl.host,
        },
      };

      targetReq = https.request(url, options, (targetRes) => {
        // Handle Redirects
        if (targetRes.statusCode === 301 || targetRes.statusCode === 302) {
          const location = targetRes.headers.location;
          if (location) {
            makeRequest(location, redirectCount + 1);
            return;
          }
        }

        // Handle expired URL or invalid signature (403/410)
        if (targetRes.statusCode === 403 || targetRes.statusCode === 410) {
          console.warn(`[stream] Upstream returned status ${targetRes.statusCode}, triggering refresh`);
          if (onExpired) {
            onExpired();
          } else {
            if (!res.headersSent) {
              res.status(targetRes.statusCode).end();
            }
          }
          return;
        }

        // Forward status code
        res.status(targetRes.statusCode);

        // Forward range and download headers
        const headersToForward = [
          "content-type",
          "content-length",
          "content-range",
          "accept-ranges",
          "cache-control",
          "expires",
        ];
        headersToForward.forEach((h) => {
          if (targetRes.headers[h]) {
            res.setHeader(h, targetRes.headers[h]);
          }
        });

        targetRes.on("error", (err) => {
          console.error(`[stream] Upstream response stream error:`, err);
          if (!closed) {
            closed = true;
            res.end();
          }
        });

        targetRes.pipe(res);
      });

      targetReq.on("error", (err) => {
        console.error(`[stream] Upstream request error:`, err);
        if (!res.headersSent) {
          res.status(502).json({ error: "Upstream request failed" });
        }
      });

      targetReq.end();
    } catch (err) {
      console.error(`[stream] Error preparing proxy request:`, err);
      if (!res.headersSent) {
        res.status(500).json({ error: "Internal URL error" });
      }
    }
  };

  makeRequest(streamUrl);

  const cleanup = () => {
    if (closed) return;
    closed = true;
    if (targetReq) {
      try {
        targetReq.destroy();
      } catch (_) {}
    }
  };

  req.on("close", cleanup);
  req.on("aborted", cleanup);
}

/**
 * GET /stream/:videoId
 *
 * Resolves the best audio track direct URL (with caching) and proxies the
 * bytes to the client. Supports seeking/ranges.
 */
router.get("/:videoId", async (req, res) => {
  const videoId = (req.params.videoId || "").toString();
  if (!/^[A-Za-z0-9_-]{6,20}$/.test(videoId)) {
    res.status(400).json({ error: "Invalid videoId" });
    return;
  }

  const getStreamInfoWithRetry = async (forceRefresh = false) => {
    if (!forceRefresh) {
      const cached = streamCache.get(videoId);
      if (cached && cached.expireMs - 300_000 > Date.now()) {
        return cached;
      }
    }

    // Resolve direct URL using yt-dlp
    console.log(`[stream] Extracting direct stream URL for ${videoId} (forceRefresh=${forceRefresh})`);
    const info = await getVideoInfo(videoId);
    // Cache stream details. Expiry is determined from YouTube's expire parameter
    const ttl = Math.max(info.expireMs - Date.now() - 300_000, 60_000);
    streamCache.set(videoId, info, ttl);
    return info;
  };

  try {
    const info = await getStreamInfoWithRetry(false);

    const range = req.headers.range;
    const requestHeaders = {
      ...info.httpHeaders,
      "User-Agent": info.httpHeaders["User-Agent"] || "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    };
    if (range) {
      requestHeaders["Range"] = range;
    }

    streamFromUrl(info.streamUrl, requestHeaders, req, res, async () => {
      // Callback triggered if direct URL returns 403/410. Clear cache and try resolving again once.
      try {
        console.log(`[stream] Direct URL expired or blocked for ${videoId}. Invalidating cache and retrying.`);
        streamCache.delete(videoId);
        const freshInfo = await getStreamInfoWithRetry(true);
        const freshHeaders = {
          ...freshInfo.httpHeaders,
          "User-Agent": freshInfo.httpHeaders["User-Agent"] || requestHeaders["User-Agent"],
        };
        if (range) {
          freshHeaders["Range"] = range;
        }
        streamFromUrl(freshInfo.streamUrl, freshHeaders, req, res, null);
      } catch (err) {
        console.error(`[stream] Retrying extraction failed for ${videoId}:`, err);
        if (!res.headersSent) {
          res.status(502).json({ error: "Retry resolution failed", detail: err.message });
        }
      }
    });

  } catch (err) {
    console.error(`[stream] Error resolved for ${videoId}:`, err);
    if (!res.headersSent) {
      res.status(500).json({ error: "Streaming resolution failed", detail: err.message });
    }
  }
});

module.exports = router;
