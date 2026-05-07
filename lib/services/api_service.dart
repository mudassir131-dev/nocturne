import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../utils/config.dart';

final apiServiceProvider = Provider<ApiService>((ref) => ApiService());

/// Thin Dio-based client for the Nocturne Node.js backend.
class ApiService {
  ApiService({Dio? dio})
      : _dio = dio ??
            Dio(BaseOptions(
              baseUrl: AppConfig.backendBaseUrl,
              connectTimeout: const Duration(seconds: 15),
              receiveTimeout: const Duration(seconds: 30),
            ));

  final Dio _dio;

  /// Search the backend `/search` route. Retries up to two times on
  /// transient network errors so flaky cellular connections don't
  /// bubble up as instant errors. Pass [cancelToken] to cancel the
  /// request when the caller's screen is disposed.
  Future<List<Song>> search(
    String query, {
    int limit = 20,
    CancelToken? cancelToken,
  }) async {
    if (query.trim().isEmpty) return const [];
    final clamped = limit.clamp(1, 25);
    var attempt = 0;
    DioException? lastError;
    while (attempt < 3) {
      attempt += 1;
      try {
        final res = await _dio.get<dynamic>(
          '/search',
          queryParameters: {'q': query, 'limit': clamped},
          cancelToken: cancelToken,
        );
        final data = res.data;
        if (data is List) {
          return data
              .whereType<Map>()
              .map((e) => Song.fromJson(Map<String, dynamic>.from(e)))
              .where((s) => s.videoId.isNotEmpty)
              .toList();
        }
        if (data is Map && data['results'] is List) {
          return (data['results'] as List)
              .whereType<Map>()
              .map((e) => Song.fromJson(Map<String, dynamic>.from(e)))
              .where((s) => s.videoId.isNotEmpty)
              .toList();
        }
        return const [];
      } on DioException catch (e) {
        if (e.type == DioExceptionType.cancel) rethrow;
        lastError = e;
        if (attempt >= 3) rethrow;
        await Future<void>.delayed(Duration(milliseconds: 400 * attempt));
      } catch (e) {
        if (kDebugMode) debugPrint('[ApiService.search] $e');
        rethrow;
      }
    }
    if (lastError != null) throw lastError;
    return const [];
  }

  /// Build a "smart shuffle" queue based on a seed song. Searches for
  /// similar tracks (artist + genre cues) and de-duplicates against the
  /// seed itself.
  Future<List<Song>> similarTo(Song seed) async {
    final queries = <String>[
      '${seed.artist} hits',
      '${seed.artist} ${seed.title.split(' ').take(2).join(' ')}',
      '${seed.artist} top songs',
    ];
    final pool = <String, Song>{seed.videoId: seed};
    for (final q in queries) {
      try {
        final results = await search(q);
        for (final s in results) {
          pool.putIfAbsent(s.videoId, () => s);
        }
      } catch (_) {
        // Continue with whatever we have.
      }
      if (pool.length > 30) break;
    }
    final list = pool.values.toList()
      ..removeWhere((s) => s.videoId == seed.videoId);
    list.shuffle();
    return [seed, ...list.take(29)];
  }

  /// Public stream URL for the given videoId (handed to just_audio).
  String streamUrl(String videoId) =>
      '${AppConfig.backendBaseUrl}/stream/$videoId';

  /// Public download URL for the given videoId.
  String downloadUrl(String videoId, {String type = 'audio'}) =>
      '${AppConfig.backendBaseUrl}/download/$videoId?type=$type';

  /// Health check used to surface backend connectivity errors early.
  Future<bool> ping() async {
    try {
      final res = await _dio.get<dynamic>('/');
      return res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }
}
