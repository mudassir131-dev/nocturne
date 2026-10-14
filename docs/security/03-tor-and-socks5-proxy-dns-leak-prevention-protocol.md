# Security & Privacy Policy 03: DNS Leak Mitigation and Remote Proxy Name Resolution Rules

## Rules
- When proxy routing is active, all DNS resolution must be offloaded to the SOCKS5 proxy server.
- OkHttp `Dns` interface implementation must intercept host lookups and pass unresolved socket addresses to prevent local OS DNS queries from leaking user IP.\n