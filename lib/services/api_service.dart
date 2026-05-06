import 'package:dio/dio.dart';
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

  /// Search YouTube via the backend `/search` route.
  Future<List<Song>> search(String query) async {
    if (query.trim().isEmpty) return const [];
    final res = await _dio.get<dynamic>(
      '/search',
      queryParameters: {'q': query},
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
