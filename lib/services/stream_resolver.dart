import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:youtube_explode_dart/youtube_explode_dart.dart';

import '../utils/config.dart';

/// Resolves a playable audio URL for a YouTube videoId by trying a chain of
/// extractors in order. The first one that returns a non-empty URL within
/// its timeout wins.
///
/// Why a chain? YouTube's bot detection now blocks `yt-dlp` on cloud IPs
/// (Railway, Fly, Heroku, …) AND `youtube_explode_dart` is intermittently
/// affected too. Public Piped/Invidious instances run their own residential
/// proxies and consistently return signed `googlevideo.com` URLs that the
/// phone can stream from directly via just_audio.
class StreamResolver {
  StreamResolver({Dio? dio}) : _dio = dio ?? Dio();

  final Dio _dio;
  final YoutubeExplode _yt = YoutubeExplode();

  /// Public Piped API instances (rotated; the first one to respond wins).
  /// Piped wraps audio URLs in its own `proxy.piped.*` domain so they're
  /// guaranteed to work from any client IP.
  static const List<String> _pipedInstances = [
    'https://api.piped.private.coffee',
    'https://pipedapi.kavin.rocks',
    'https://pipedapi.adminforge.de',
    'https://pipedapi.r4fo.com',
  ];

  /// Public Invidious API instances. Returns direct `googlevideo.com` URLs
  /// (with `&ipbypass=yes` so any client can fetch them).
  static const List<String> _invidiousInstances = [
    'https://invidious.f5.si',
    'https://inv.thepixora.com',
    'https://invidious.einfachzocken.eu',
  ];

  Future<String?> resolve(String videoId) async {
    // 1) Piped (most reliable when public instances are healthy)
    for (final base in _pipedInstances) {
      final url = await _runWithTimeout(
        () => _resolveViaPiped(base, videoId),
        seconds: 6,
        label: 'piped@$base',
      );
      if (url != null) return url;
    }
    // 2) Invidious
    for (final base in _invidiousInstances) {
      final url = await _runWithTimeout(
        () => _resolveViaInvidious(base, videoId),
        seconds: 6,
        label: 'invidious@$base',
      );
      if (url != null) return url;
    }
    // 3) On-device extraction
    final url = await _runWithTimeout(
      () => _resolveViaYoutubeExplode(videoId),
      seconds: 10,
      label: 'youtube_explode',
    );
    if (url != null) return url;

    // 4) Last-resort backend fallback (only useful if YTDLP_COOKIES_BASE64 is set)
    final fallback = '${AppConfig.backendBaseUrl}/stream/$videoId';
    if (kDebugMode) debugPrint('[StreamResolver] using backend fallback: $fallback');
    return fallback;
  }

  void dispose() {
    _yt.close();
  }

  Future<String?> _runWithTimeout(
    Future<String?> Function() body, {
    required int seconds,
    required String label,
  }) async {
    try {
      final r = await body().timeout(Duration(seconds: seconds));
      if (r != null && kDebugMode) debugPrint('[StreamResolver] $label OK');
      return r;
    } catch (e) {
      if (kDebugMode) debugPrint('[StreamResolver] $label failed: $e');
      return null;
    }
  }

  Future<String?> _resolveViaPiped(String base, String videoId) async {
    final res = await _dio.get<dynamic>(
      '$base/streams/$videoId',
      options: Options(
        responseType: ResponseType.plain,
        followRedirects: true,
        validateStatus: (c) => c != null && c < 500,
      ),
    );
    if (res.statusCode != 200 || res.data == null) return null;
    final data = jsonDecode(res.data as String);
    if (data is! Map<String, dynamic>) return null;
    final List<dynamic>? streams = data['audioStreams'] as List<dynamic>?;
    if (streams == null || streams.isEmpty) return null;
    final picked = _pickHighestBitrate(
      streams.cast<Map<String, dynamic>>(),
      bitrateKey: 'bitrate',
      urlKey: 'url',
    );
    return picked;
  }

  Future<String?> _resolveViaInvidious(String base, String videoId) async {
    final res = await _dio.get<dynamic>(
      '$base/api/v1/videos/$videoId',
      options: Options(
        responseType: ResponseType.plain,
        followRedirects: true,
        validateStatus: (c) => c != null && c < 500,
      ),
    );
    if (res.statusCode != 200 || res.data == null) return null;
    final data = jsonDecode(res.data as String);
    if (data is! Map<String, dynamic>) return null;
    final List<dynamic>? formats = data['adaptiveFormats'] as List<dynamic>?;
    if (formats == null || formats.isEmpty) return null;
    final audio = formats
        .cast<Map<String, dynamic>>()
        .where((f) => (f['type'] as String? ?? '').startsWith('audio'))
        .toList();
    if (audio.isEmpty) return null;
    return _pickHighestBitrate(
      audio,
      bitrateKey: 'bitrate',
      urlKey: 'url',
    );
  }

  Future<String?> _resolveViaYoutubeExplode(String videoId) async {
    final manifest = await _yt.videos.streamsClient.getManifest(videoId);
    final audioOnly = manifest.audioOnly.toList();
    if (audioOnly.isNotEmpty) {
      audioOnly.sort((a, b) =>
          b.bitrate.bitsPerSecond.compareTo(a.bitrate.bitsPerSecond));
      return audioOnly.first.url.toString();
    }
    final muxed = manifest.muxed.toList();
    if (muxed.isNotEmpty) {
      muxed.sort((a, b) =>
          b.bitrate.bitsPerSecond.compareTo(a.bitrate.bitsPerSecond));
      return muxed.first.url.toString();
    }
    return null;
  }

  String? _pickHighestBitrate(
    List<Map<String, dynamic>> streams, {
    required String bitrateKey,
    required String urlKey,
  }) {
    int bitrateOf(Map<String, dynamic> s) {
      final v = s[bitrateKey];
      if (v is int) return v;
      if (v is double) return v.toInt();
      if (v is String) return int.tryParse(v) ?? 0;
      return 0;
    }

    streams.sort((a, b) => bitrateOf(b).compareTo(bitrateOf(a)));
    final url = streams.first[urlKey];
    if (url is String && url.isNotEmpty) return url;
    return null;
  }
}
