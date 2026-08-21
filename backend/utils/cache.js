"use strict";

class MemoryCache {
  constructor() {
    this.store = new Map();
  }

  /**
   * Get value from cache. Returns null if missing or expired.
   */
  get(key) {
    const entry = this.store.get(key);
    if (!entry) return null;

    if (Date.now() > entry.expiry) {
      this.store.delete(key);
      return null;
    }

    return entry.value;
  }

  /**
   * Set cache entry with a TTL (Time To Live) in milliseconds.
   */
  set(key, value, ttlMs) {
    const expiry = Date.now() + ttlMs;
    this.store.set(key, { value, expiry });
  }

  /**
   * Remove a single entry.
   */
  delete(key) {
    this.store.delete(key);
  }

  /**
   * Clear the entire cache store.
   */
  clear() {
    this.store.clear();
  }

  /**
   * Cleanup expired keys manually to avoid memory creep.
   */
  prune() {
    const now = Date.now();
    for (const [key, entry] of this.store.entries()) {
      if (now > entry.expiry) {
        this.store.delete(key);
      }
    }
  }
}

// Global cache instances for different endpoints
const streamCache = new MemoryCache();
const searchCache = new MemoryCache();

// Run pruning every 10 minutes
setInterval(() => {
  streamCache.prune();
  searchCache.prune();
}, 10 * 60 * 1000).unref();

module.exports = {
  streamCache,
  searchCache,
  MemoryCache,
};
