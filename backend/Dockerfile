# syntax=docker/dockerfile:1.6

# Nocturne backend: Express server that shells out to yt-dlp.
# Includes ffmpeg for mp3 conversion in /download.
#
# We pin to Debian Bookworm (Debian 12, current stable) so apt mirrors
# remain healthy on free-tier hosts. The previous bullseye image started
# 404-ing on libgbm1/libopenmpt as packages got archived.
FROM node:20-bookworm-slim

ENV DEBIAN_FRONTEND=noninteractive

# Retry on transient mirror failures and skip recommended packages we
# don't need (e.g. X11 libs that ffmpeg pulls in by default).
RUN set -eux; \
    echo 'Acquire::Retries "5";' > /etc/apt/apt.conf.d/80-retries; \
    apt-get update; \
    apt-get install -y --no-install-recommends --fix-missing \
        ca-certificates \
        curl \
        ffmpeg \
        python3; \
    curl -fsSL https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp \
         -o /usr/local/bin/yt-dlp; \
    chmod +x /usr/local/bin/yt-dlp; \
    /usr/local/bin/yt-dlp --version; \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY package*.json ./
RUN npm install --omit=dev

COPY . .

ENV PORT=3000
EXPOSE 3000

CMD ["node", "index.js"]
