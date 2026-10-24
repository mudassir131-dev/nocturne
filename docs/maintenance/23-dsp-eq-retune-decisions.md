# 23 - DSP EQ Retune Decisions

Documents the equalization engine retune that produced the
"Apple-Music-lossless-feel" signature requested for Nocturne's enhancing
pipeline (decoder output → `AudioDspProcessor` → sink).

## Parametric section

| Band | Before | After | Rationale |
|---|---|---|---|
| Low shelf | 80 Hz, Q 0.707 | **65 Hz, Q 0.85** | Puts the punch on the sub shelf; the slightly steeper Q keeps the boost tight instead of range-y, avoiding 100–200 Hz boom |
| Presence | 3.2 kHz, Q 1.0 | **2.6 kHz, Q 0.9** | Vocal body and intimacy below the 3.3 kHz+ sibilance zone, so boosted clarity stays smooth on bright earphones |
| Low mid / air | unchanged | unchanged | 320 Hz mud control and the 11 kHz air shelf already sat where they should |

## Preset curves

| Preset | Bass / Clarity / Treble | Notes |
|---|---|---|
| BALANCED | 3.0 / 1.5 / 0.7 dB | Punchy sub-bass foundation, forward yet smooth presence, modest air |
| BASS_BOOST | 5.0 / 0.8 / 0.3 dB | Deeper shelf with a small clarity reserve so heavy low end never buries the vocal line |
| VOCAL | 0.5 / 2.5 / 1.0 dB | Small bass foundation keeps vocals anchored instead of thin |
| LOUD_CLEAN | 2.5 / 1.0 / 0.5 dB | Kept as-is (already tuned during the loudness work) |

## Dynamic bass management

The envelope-following sidechain attenuation is stronger on bass-heavy
presets: `0.35` for BASS_BOOST, `0.25` for BALANCED, `0.20` for everything
else — punch without boom or limiter pumping. Non-vocal presets now count on
a `−0.6 dB` low-mid notch (was −0.4) to keep mids clean under a boosted
shelf.

## Verification

`AudioDspProcessorTest` pins the retuned preset values and the coefficient
frequencies so a future edit cannot silently drift the signature.
