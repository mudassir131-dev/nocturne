import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';

/// Free, key-less lyrics provider backed by [lrclib.net](https://lrclib.net).
///
/// `getSynced` returns a list of `(timestamp, line)` pairs parsed from the
/// LRC body the API returns. If the API has no synced match for a track,
/// the plain-text fallback is wrapped in a single zero-timestamp entry so
/// the lyrics screen can still display something useful.
final lyricsServiceProvider = Provider<LyricsService>((_) => LyricsService());

class LyricsLine {
  final Duration time;
  final String text;
  const LyricsLine(this.time, this.text);
}

class Lyrics {
  final List<LyricsLine> lines;
  final bool synced;
  const Lyrics({required this.lines, required this.synced});

  bool get isEmpty => lines.isEmpty;
}

class LyricsService {
  LyricsService({Dio? dio})
      : _dio = dio ??
            Dio(BaseOptions(
              connectTimeout: const Duration(seconds: 4),
              receiveTimeout: const Duration(seconds: 8),
              headers: {
                // lrclib asks API consumers to identify themselves so
                // they can rate-limit abusive clients separately.
                'User-Agent':
                    'Nocturne/0.3 (https://github.com/mudassir131-dev/nocturne)',
              },
            ));

  final Dio _dio;
  final Map<String, Lyrics> _cache = {};

  /// Fetch lyrics for [song]. Returns an empty [Lyrics] (not null) when no
  /// match was found so callers can render a "No lyrics yet" placeholder.
  Future<Lyrics> getLyrics(Song song) async {
    final key = '${song.title}|${song.artist}';
    final cached = _cache[key];
    if (cached != null) return cached;

    try {
      final res = await _dio.get<dynamic>(
        'https://lrclib.net/api/get',
        queryParameters: {
          'track_name': song.title,
          'artist_name': song.artist,
          if (song.duration != null) 'duration': song.duration!.inSeconds,
        },
        options: Options(
          validateStatus: (c) => c != null && c < 500,
          followRedirects: true,
        ),
      );
      if (res.statusCode != 200 || res.data is! Map) {
        // Try a relaxed search if the strict get returned 404.
        return _searchFallback(song, key);
      }
      final lyrics = _parseFromMap(res.data as Map<String, dynamic>);
      _cache[key] = lyrics;
      return lyrics;
    } catch (_) {
      return _searchFallback(song, key);
    }
  }

  Future<Lyrics> _searchFallback(Song song, String key) async {
    try {
      final res = await _dio.get<dynamic>(
        'https://lrclib.net/api/search',
        queryParameters: {
          'q': '${song.title} ${song.artist}'.trim(),
        },
        options: Options(
          validateStatus: (c) => c != null && c < 500,
          followRedirects: true,
        ),
      );
      final data = res.data;
      if (data is List && data.isNotEmpty) {
        final first = data.first;
        if (first is Map) {
          final lyrics = _parseFromMap(Map<String, dynamic>.from(first));
          _cache[key] = lyrics;
          return lyrics;
        }
      }
    } catch (_) {/* fall through to empty */}
    final empty = const Lyrics(lines: [], synced: false);
    _cache[key] = empty;
    return empty;
  }

  Lyrics _parseFromMap(Map<String, dynamic> data) {
    final synced = (data['syncedLyrics'] ?? '').toString();
    if (synced.isNotEmpty) {
      return Lyrics(lines: _parseLrc(synced), synced: true);
    }
    final plain = (data['plainLyrics'] ?? '').toString();
    if (plain.isNotEmpty) {
      return Lyrics(
        lines: plain
            .split('\n')
            .where((l) => l.trim().isNotEmpty)
            .map((l) => LyricsLine(Duration.zero, l.trim()))
            .toList(),
        synced: false,
      );
    }
    return const Lyrics(lines: [], synced: false);
  }

  /// Parses an LRC body like:
  ///   [00:12.34]Line one
  ///   [00:15.67]Line two
  /// into a sorted list of [LyricsLine].
  List<LyricsLine> _parseLrc(String body) {
    final timeTag = RegExp(r'\[(\d{1,3}):(\d{1,2})(?:\.(\d{1,3}))?\]');
    final lines = <LyricsLine>[];
    for (final raw in body.split(RegExp(r'\r?\n'))) {
      final line = raw.trim();
      if (line.isEmpty) continue;
      final matches = timeTag.allMatches(line).toList();
      if (matches.isEmpty) continue;
      final text = line.substring(matches.last.end).trim();
      if (text.isEmpty) continue;
      for (final m in matches) {
        final mins = int.tryParse(m.group(1) ?? '0') ?? 0;
        final secs = int.tryParse(m.group(2) ?? '0') ?? 0;
        final fracStr = m.group(3) ?? '0';
        final frac = int.tryParse(fracStr.padRight(3, '0')) ?? 0;
        final ms = mins * 60 * 1000 + secs * 1000 + frac;
        lines.add(LyricsLine(Duration(milliseconds: ms), text));
      }
    }
    lines.sort((a, b) => a.time.compareTo(b.time));
    return lines;
  }
}
