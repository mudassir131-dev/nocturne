import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';

final lyricsServiceProvider = Provider<LyricsService>((_) => LyricsService());

/// Fetches plain-text lyrics for a [Song] from public sources.
///
/// Primary: lyrics.ovh — public, no API key needed. Returns plain
/// English/transliterated lyrics for most popular tracks.
///
/// We strip common YouTube title noise ("(Official Video)",
/// "[Audio]", "feat. ...") before querying, and fall back to a
/// title-only lookup if artist+title returns nothing.
class LyricsService {
  LyricsService({Dio? dio})
      : _dio = dio ??
            Dio(BaseOptions(
              connectTimeout: const Duration(seconds: 8),
              receiveTimeout: const Duration(seconds: 12),
              validateStatus: (c) => c != null && c < 500,
            ));

  final Dio _dio;

  Future<String?> fetch(Song song) async {
    final artist = _cleanArtist(song.artist);
    final title = _cleanTitle(song.title);

    final attempts = <(String, String)>[
      (artist, title),
      ('', title),
    ];

    for (final (a, t) in attempts) {
      if (t.trim().isEmpty) continue;
      final lyric = await _query(a, t);
      if (lyric != null && lyric.trim().isNotEmpty) return lyric;
    }
    return null;
  }

  Future<String?> _query(String artist, String title) async {
    try {
      final url = 'https://api.lyrics.ovh/v1/'
          '${Uri.encodeComponent(artist)}/'
          '${Uri.encodeComponent(title)}';
      final res = await _dio.get<dynamic>(url);
      if (res.statusCode != 200 || res.data is! Map) return null;
      final raw = (res.data as Map)['lyrics'];
      if (raw is String) return raw.trim();
      return null;
    } catch (_) {
      return null;
    }
  }

  static String _cleanTitle(String s) {
    var t = s;
    final patterns = [
      RegExp(r'\([^)]*\)'),
      RegExp(r'\[[^\]]*\]'),
      RegExp(r'\bofficial\s+(music\s+)?(video|audio|lyric|lyrics)\b',
          caseSensitive: false),
      RegExp(r'\bfeat\.?\s+[^|–\-]+', caseSensitive: false),
      RegExp(r'\bft\.?\s+[^|–\-]+', caseSensitive: false),
      RegExp(r'\b(hd|4k|hq|remastered|lyrics?)\b', caseSensitive: false),
    ];
    for (final p in patterns) {
      t = t.replaceAll(p, ' ');
    }
    return t.replaceAll(RegExp(r'\s+'), ' ').trim();
  }

  static String _cleanArtist(String s) {
    var t = s;
    final patterns = [
      RegExp(r'\bvevo\b', caseSensitive: false),
      RegExp(r'\bofficial\b', caseSensitive: false),
      RegExp(r'\btopic\b', caseSensitive: false),
      RegExp(r'-\s*topic', caseSensitive: false),
    ];
    for (final p in patterns) {
      t = t.replaceAll(p, ' ');
    }
    return t.replaceAll(RegExp(r'\s+'), ' ').trim();
  }
}
