# Echo Apple Player UI Forensic Diff

Source of truth: Echo Music commit `6bea1dbf25c5761707ac0d5c9f3ce32d589be7aa`.

## Composition mapping

| Visual responsibility | Echo donor source | Existing Nocturne Apple host | Required transplant |
| --- | --- | --- | --- |
| Full-screen backdrop | `BottomSheetPlayer` `PlayerBackgroundStyle.APPLE_MUSIC` branch (`Player.kt`:1128-1221) | A 72 dp blurred image plus an opaque three-stop gradient inside `AppleExpandedPlayer` | Use Echo's full-size 128 px blurred artwork, top 65% clear artwork, `DstIn` fade mask, canvas overlay, and final black gradient. |
| Default artwork stage | Portrait `Box(weight(1f))` + `Thumbnail`; `Thumbnail` is hidden for `APPLE_MUSIC` in portrait (`Thumbnail.kt`:394) so the backdrop is the visible artwork | A square `Surface` with 12 dp corners and 1:1 aspect ratio | Remove the card. Keep the weighted stage and let the full-bleed backdrop occupy it. |
| Live artwork | `BackgroundVideoView` fills the clear top-65% artwork layer (`Player.kt`:1196-1205) | `CanvasArtworkPlayer` fills the square card | Put Nocturne's existing live-art resolver/player behind the same top-65% fade mask as Echo. |
| Lyrics state | `AnimatedContent(showInlineLyrics)` replaces the portrait artwork stage with `InlineLyricsView`; clear artwork fades to zero (`Player.kt`:1158-1168, 2819-2831) | Lyrics are drawn as an overlay inside the square card | Preserve Nocturne's provider/timing backend, but make lyrics a distinct stage selected by the Echo toggle and fade out clear artwork in that mode. |
| Metadata | `controlsContent` row, 32 dp horizontal padding; bold `titleLarge`, 4 dp gap, 16 sp artist (`Player.kt`:1381-1597) | Similar text, but positioned after the square card with 28 dp padding | Reuse Echo row spacing, typography, and bottom-overlay relationship. |
| More/favorite actions | Old Apple branch: separate 40 dp circular 20%-white buttons, 12 dp apart; overflow first, favorite second (`Player.kt`:1738-1868) | Bare `IconButton`s; favorite first, overflow second | Restore Echo order, size, shape, tint, and conditional lyrics actions. |
| Progress | Echo slider after a 20 dp metadata gap; 32 dp horizontal padding (`Player.kt`:1873-2028) | Unstyled full-width slider immediately after metadata | Restore donor spacing and horizontal bounds. |
| Time/quality row | 36 dp horizontal padding; elapsed left, optional codec/timer badge center, duration right (`Player.kt`:2029-2212) | Elapsed left and negative remaining time right; no quality badge | Show elapsed/duration and adapt `PlayerConnection.currentFormat` into Echo's codec/bitrate badge. |
| Transport | Old Apple branch: 48 dp skip icons, 100 dp play hit area, 72 dp play/pause art (`Player.kt`:2443-2549) | 64/76 dp generic `IconButton` hit areas and 42/54 dp art | Transplant donor dimensions and layout. |
| Volume/output | Echo volume slider is conditional (`!hidePlayerSlider`); it is absent in the supplied target state. Output selection lives in the bottom grouped action (`Player.kt`:2556-2685; `Queue.kt`:513-546) | Volume slider and AirFlow/device label are permanently in the main column | Remove it from the default composition; retain output functionality in the donor bottom grouped action. |
| Bottom actions | Old Apple `Queue.collapsedContent`: 30 dp horizontal/12 dp vertical, queue button left, connected 120 x 56 dp output/sleep group center, lyrics button right, bottom system inset (`Queue.kt`:479-625) | Divider plus three evenly spaced lyrics/output/queue icons | Transplant donor order, sizes, grouping, and navigation-bar inset. |
| Insets | Background is full bleed; action strip applies bottom + horizontal system insets; Echo forces light status icons over Apple background | Whole custom column uses arbitrary 16 dp vertical padding and no donor action inset model | Keep artwork behind the status bar and apply navigation inset only to the bottom action strip. |

## Root causes

The former host copied backend capabilities into a newly invented hierarchy instead of preserving Echo's hierarchy. It made the artwork and lyrics share a permanent rounded card, moved output into an always-visible volume section, inverted the metadata action order, omitted the quality badge, and replaced `Queue.collapsedContent` with a generic three-icon footer. Those decisions account for the device-level mismatch even though the underlying actions worked.

## Adaptation boundary

The transplanted composables consume Nocturne state only through `NocturneApplePlayerAdapter` and explicit callbacks. The existing lyrics providers/parsers, live-art resolution chain, playback, queue, timer, output sheet, menu, Search/Navigation changes, Cinematic player, and mini player remain outside the visual rewrite.
