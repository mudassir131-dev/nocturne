# Echo Apple-player integration map

Upstream: Echo Music `6bea1dbf25c5761707ac0d5c9f3ce32d589be7aa` (GPL-3.0).

This document records the integration incrementally. The existing Nocturne
`BottomSheetPlayer` and the protected files listed in
`scripts/protected-player.sha256` are outside the modification boundary.

## Host boundary

The existing Nocturne player remains the default. `MainActivity` selects between
the unchanged player and an isolated Apple player host. The Apple implementation
maps Nocturne playback/database state through a dedicated adapter.

## Donor mapping

| Subsystem | Echo source | Nocturne destination |
|---|---|---|
| Apple player branch | `ui/player/Player.kt` (`useNewPlayerDesign == false`) | `ui/appleplayer` |
| Apple queue/actions | `ui/player/Queue.kt`, `ui/menu/OldPlayerMenu.kt` | `ui/appleplayer/ApplePlayerHost.kt` plus Nocturne's real `PlayerMenu` actions |
| Lyrics registry/providers | `lyrics`, `youlyplus`, `paxsenixlyrics`, `unison` | isolated Apple lyrics packages/modules |
| Live artwork | `canvas`, `echomusiccanvas`, `applecanvas` | separately named Apple-player provider packages/modules |
| Audio devices | `echomusic/AudioDeviceBottomSheet.kt` | isolated Apple-player device UI |

Only controls backed by real Nocturne or fully transplanted functionality are
shown. Provider word/syllable timing is preserved; synthetic timing is forbidden.

## Implemented isolation points

- `PlayerExperience.NOCTURNE` is the default; the preference is evaluated only
  by `MainActivity`. No legacy `PlayerDesignStyle` value is changed.
- `NocturneApplePlayerAdapter` owns playback actions for the new host.
- The Apple queue supports selection, play-next reordering, removal, shuffle,
  repeat, and favorite actions through Media3/Nocturne APIs.
- Lyrics resolve sequentially in the documented nine-provider order. Rich LRC,
  Paxsenix word payloads, and TTML spans retain provider timestamps. Line-only
  input never receives generated word timing.
- Live artwork resolves Echo manifest, then Tidal MP4, then Apple AMP HLS, with
  static art beneath the first-frame fade. The renderer is muted, looping, and
  released on composition disposal. Data Saver bypasses all motion requests.
- The audio sheet reports real Android output devices and controls the real
  `STREAM_MUSIC` volume. The donor's empty audio-quality backend is not exposed.
- The Apple overflow includes favorite, shuffle, repeat, queue, sleep timer,
  native sharing, and Nocturne's real extended song actions (radio, playlists,
  downloads, artist/album navigation, details, equalizer, tempo, and Music Together).
- Search remains in `Screens.MainScreens` for routing/rail state, while the
  bottom bar renders it as a sibling circular action. The query-empty online
  search surface uses Nocturne discovery, `ForYouSuggestionEngine`, and new
  release albums while retaining search history and typed-query routing.

## Verification

Run `scripts/verify-protected-player.ps1` after every checkpoint. Both
`compileUniversalDebugKotlin` and `compileArmeabiDebugKotlin` pass with Android
SDK 36. APK assembly reaches `compileUniversalDebugJavaWithJavac`, then the
Codex Windows sandbox denies ZipFS archive cleanup for a Gradle-transformed
Compose JAR (`AccessDeniedException` on `foundation-layout-api.jar`). The same
failure reproduces with Microsoft and Temurin JDK 21 and with serial workers;
the downstream Javac missing-symbol messages are cascading classpath failures.
No APK or device-runtime result is claimed from this environment.
