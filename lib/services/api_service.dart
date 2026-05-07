import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../utils/config.dart';

final apiServiceProvider = Provider<ApiService>((ref) => ApiService());

/// Thin Dio-based client for the Nocturne Node.js backend.
///
/// All GETs go through [_get], which retries 3 times with a small
/// exponential back-off (250ms / 600ms / 1.2s) and uses a 30-second
/// receive timeout. This keeps slow-internet sessions from failing
/// outright while still surfacing real backend outages quickly.
class ApiService {
  ApiService({Dio? dio})
      : _dio = dio ??
            Dio(BaseOptions(
              baseUrl: AppConfig.backendBaseUrl,
              connectTimeout: const Duration(seconds: 15),
              receiveTimeout: const Duration(seconds: 30),
              sendTimeout: const Duration(seconds: 15),
              headers: {
                if (AppConfig.backendAuthHeader != null)
                  'Authorization': AppConfig.backendAuthHeader!,
              },
            ));

  final Dio _dio;

  static const _maxAttempts = 3;
  static const _baseBackoff = Duration(milliseconds: 250);

  Future<Response<dynamic>> _get(
    String path, {
    Map<String, dynamic>? queryParameters,
  }) async {
    Object? lastError;
    for (var attempt = 0; attempt < _maxAttempts; attempt++) {
      try {
        return await _dio.get<dynamic>(
          path,
          queryParameters: queryParameters,
        );
      } on DioException catch (e) {
        lastError = e;
        if (!_shouldRetry(e) || attempt == _maxAttempts - 1) rethrow;
        if (kDebugMode) {
          debugPrint(
            '[api] retry $path attempt=${attempt + 1} err=${e.type}',
          );
        }
        await Future<void>.delayed(_baseBackoff * (1 << attempt));
      }
    }
    throw lastError ?? StateError('api: exhausted retries');
  }

  bool _shouldRetry(DioException e) {
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.receiveTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.connectionError:
        return true;
      case DioExceptionType.badResponse:
        final code = e.response?.statusCode ?? 0;
        return code >= 500;
      default:
        return false;
    }
  }

  /// Search YouTube via the backend `/search` route.
  Future<List<Song>> search(String query) async {
    if (query.trim().isEmpty) return const [];
    final res = await _get('/search', queryParameters: {'q': query});
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
      final res = await _dio.get<dynamic>(
        '/',
        options: Options(
          receiveTimeout: const Duration(seconds: 5),
        ),
      );
      return res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }
}
