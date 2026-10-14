# Troubleshooting Guide 14: Troubleshooting Tor SOCKS5 Proxy Timeouts and DNS Leaks

## Symptom Overview
When "Route through Tor / SOCKS5" is enabled in Nocturne Network Settings, search results fail with `SocketTimeoutException` or DNS queries leak outside the proxy tunnel.

## Root Cause
- SOCKS5 proxy configured without remote DNS resolution (`SocksProxyType.SOCKS5` resolving DNS locally before socket connection).
- Orbot or local Tor daemon not listening on `127.0.0.1:9050`.

## Diagnostic Steps
1. Test local proxy socket connectivity:
   ```bash
   adb shell nc -zv 127.0.0.1 9050
   ```
2. Verify OkHttp DNS configuration routes lookups through SOCKS proxy:
   ```kotlin
   val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 9050))
   ```

## Fix Implementation
- Always enforce remote DNS resolution by passing unresolved `InetSocketAddress.createUnresolved(host, port)`.
- Provide user-friendly in-app toast notification if the configured proxy port is unreachable.\n