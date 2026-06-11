"use strict";

const https = require("https");
const fs = require("fs");
const path = require("path");

const BIN_DIR = path.join(__dirname, "..", "bin");
if (!fs.existsSync(BIN_DIR)) {
  fs.mkdirSync(BIN_DIR, { recursive: true });
}

const platform = process.platform;
let binaryName = "yt-dlp";
let url = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp";

if (platform === "win32") {
  binaryName = "yt-dlp.exe";
  url = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";
} else if (platform === "darwin") {
  url = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_macos";
}

const dest = path.join(BIN_DIR, binaryName);

function download(url, destPath) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      if (res.statusCode === 301 || res.statusCode === 302) {
        download(res.headers.location, destPath).then(resolve).catch(reject);
        return;
      }
      if (res.statusCode !== 200) {
        reject(new Error(`Failed to download from ${url}. Status code: ${res.statusCode}`));
        return;
      }
      const fileStream = fs.createWriteStream(destPath);
      res.pipe(fileStream);
      fileStream.on("finish", () => {
        fileStream.close();
        resolve();
      });
      fileStream.on("error", (err) => {
        fs.unlink(destPath, () => {});
        reject(err);
      });
    }).on("error", (err) => {
      reject(err);
    });
  });
}

async function main() {
  if (fs.existsSync(dest)) {
    console.log(`[setup] yt-dlp binary already exists at ${dest}, skipping download.`);
    return;
  }
  console.log(`[setup] Downloading yt-dlp for ${platform} from ${url}...`);
  try {
    await download(url, dest);
    console.log(`[setup] yt-dlp successfully downloaded to ${dest}`);
    if (platform !== "win32") {
      fs.chmodSync(dest, "755");
      console.log(`[setup] Set execution permissions for ${dest}`);
    }
  } catch (err) {
    console.error(`[setup] Error downloading yt-dlp:`, err);
    process.exit(1);
  }
}

main();
