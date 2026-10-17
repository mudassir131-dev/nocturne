# Networking 17: Metered Network Bandwidth Throttling and Low-Bitrate Mode

## Data Saver
- Interrogates ConnectivityManager NetworkCapabilities for NET_CAPABILITY_NOT_METERED.
- Automatically forces 48kbps Opus bitrate when on metered mobile cellular connections.

