# Echo Music attribution

Nocturne includes adapted portions of Echo Music:

- Upstream project: <https://github.com/EchoMusicApp/Echo-Music>
- Audited source commit: `6bea1dbf25c5761707ac0d5c9f3ce32d589be7aa`
- Upstream license: GNU General Public License, version 3

The integration is adapted for Nocturne's package namespace, playback service,
Room database, navigation, preferences, and UI host. The transplanted scope is
limited to the optional Apple-inspired player and its required queue, lyrics,
audio-device, and live-artwork functionality. Nocturne's existing player remains
the default and is maintained as an unchanged subsystem.

Files adapted from Echo retain an SPDX GPL-3.0-or-later header and an upstream
origin note. Provider modules also retain their applicable upstream notices.

Known donor behaviors intentionally not reproduced:

- synthetic character-count word timing for lyrics without provider timestamps;
- the audio-quality selector whose donor backend is empty;
- accidental first-network-response lyrics-provider selection.

See `docs/ECHO_INTEGRATION.md` for the detailed source-to-host mapping as the
implementation progresses.
