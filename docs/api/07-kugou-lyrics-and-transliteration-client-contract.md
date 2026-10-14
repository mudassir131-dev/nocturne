# Subsystem API Specification 07: Kugou Candidate Search and QRC Decoding API Contract

## Protocol Steps
1. Search candidate lyric IDs via `http://lyrics.kugou.com/search`.
2. Retrieve Base64 encoded and DES/AES encrypted QRC lyric payload via `http://lyrics.kugou.com/download`.
3. Decrypt payload using Kugou static key table.
4. Parse word timestamps and Romaji/Pinyin transliteration tracks.\n