# Networking 04: DNS-over-HTTPS (DoH) Fallback Resolver for Strict Networks

## Resolution Path
- Fallback to Cloudflare (`1.1.1.1`) or Google (`8.8.8.8`) DoH when local ISP DNS fails or blocks queries.
- Prevents DNS hijacking and unblocks streaming access across restricted network environments.
