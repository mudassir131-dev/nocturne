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

  /// Public Piped API instances. We race them in parallel so the first
  /// healthy one wins; dead/degraded instances no longer block the chain.
  static const List<String> _pipedInstances = [
    'https://api.piped.private.coffee',
    'https://pipedapi.kavin.rocks',
    'https://pipedapi.adminforge.de',
  ];

  /// Public Invidious API instances. Also raced in parallel.
  static const List<String> _invidiousInstances = [
    'https://invidious.f5.si',
    'https://invidious.einfachzocken.eu',
  ];

  Future<String?> resolve(String videoId) async {
    // 1) Race all Piped + Invidious instances in parallel; first one to
    //    return a non-null URL wins. Each call has its own short timeout
    //    so dead hosts can't stall the chain (~3s per attempt).
    final racers = <Future<String?>>[
      for (final base in _pipedInstances)
        _runWithTimeout(
          () => _resolveViaPiped(base, videoId),
          seconds: 4,
          label: 'piped@$base',
        ),
      for (final base in _invidiousInstances)
        _runWithTimeout(
          () => _resolveViaInvidious(base, videoId),
          seconds: 4,
          label: 'invidious@$base',
        ),
    ];
    final winner = await _firstNonNull(racers).timeout(
      const Duration(seconds: 6),
      onTimeout: () => null,
    );
    if (winner != null) return winner;

    // 2) On-device extraction
    final viaYt = await _runWithTimeout(
      () => _resolveViaYoutubeExplode(videoId),
      seconds: 8,
      label: 'youtube_explode',
    );
    if (viaYt != null) return viaYt;

    // 3) Last-resort backend fallback (only useful if YTDLP_COOKIES_BASE64 is set)
    final fallback = '${AppConfig.backendBaseUrl}/stream/$videoId';
    if (kDebugMode) debugPrint('[StreamResolver] using backend fallback: $fallback');
    return fallback;
  }

  /// Returns the first non-null result among [futures], or null if all
  /// resolve to null. Doesn't cancel the other in-flight requests (they
  /// just complete in the background and are discarded).
  Future<String?> _firstNonNull(List<Future<String?>> futures) {
    final completer = Completer<String?>();
    var pending = futures.length;
    if (pending == 0) {
      completer.complete(null);
      return completer.future;
    }
    for (final f in futures) {
      f.then((value) {
        if (completer.isCompleted) return;
        if (value != null && value.isNotEmpty) {
          completer.complete(value);
          return;
        }
        pending -= 1;
        if (pending == 0 && !completer.isCompleted) {
          completer.complete(null);
        }
      }).catchError((_) {
        if (completer.isCompleted) return;
        pending -= 1;
        if (pending == 0 && !completer.isCompleted) {
          completer.complete(null);
        }
      });
    }
    return completer.future;
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
