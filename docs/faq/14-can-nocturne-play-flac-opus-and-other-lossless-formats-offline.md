# FAQ: Can Nocturne play FLAC, Opus, and other lossless formats offline?

## Question
Can Nocturne play FLAC, Opus, and other lossless formats offline?

## Answer
Yes. Nocturne includes full native decoders for FLAC, ALAC, Opus, AAC, Vorbis, and MP3. Local audio files stored on device storage or SD cards are scanned by the MediaStore scanner and played through the 32-bit floating-point audio engine with high-res DAC support.

### What bitrate does online streaming use per song?

Since v2.22.39 every quality mode resolves the per-song verified catalog first
(self-hosted lossless and the Saavn 320 kbps master) whenever Data Saver is
off, so a verified match streams at its own bitrate — lossless stays lossless,
and Saavn's `_96/_160` tokens upgrade to `_320`. Songs without a verified
catalog match keep playing the exact same `videoId` through YouTube at the
highest bitrate YouTube offers for that upload; for typical uploads that
ceiling is the `itag 251` Opus at ~160 kbps, which is YouTube's own maximum
upstream rather than a Nocturne limit. See
`docs/maintenance/25-raw-max-bitrate-per-song-playback-policy.md`.
