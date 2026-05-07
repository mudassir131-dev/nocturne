import 'dart:async';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive/hive.dart';
import 'package:path_provider/path_provider.dart';

import '../models/song.dart';
import '../utils/config.dart';

final downloadServiceProvider =
    Provider<DownloadService>((ref) => DownloadService());

/// Manages offline song downloads (MP3 via the backend `/download`
/// endpoint) and persists download metadata in the `downloads` Hive box.
///
/// Downloads land in the app's documents directory, so they survive
/// across app restarts but are removed cleanly on uninstall.
class DownloadService {
  DownloadService({Dio? dio}) : _dio = dio ?? Dio();

  final Dio _dio;
  final Map<String, double> _progress = {};
  final StreamController<Map<String, double>> _progressController =
      StreamController<Map<String, double>>.broadcast();

  Box<dynamic> get _box => Hive.box<dynamic>('downloads');

  /// Real-time progress map (videoId -> 0..1) keyed by videoId. The map
  /// only contains in-flight downloads; completed entries are removed.
  Stream<Map<String, double>> get progressStream =>
      _progressController.stream;

  Map<String, double> get currentProgress => Map.unmodifiable(_progress);

  /// Returns the local path of a downloaded song, or null if not
  /// downloaded.
  String? localPathFor(String videoId) {
    final entry = _box.get(videoId);
    if (entry is Map && entry['path'] is String) {
      final p = entry['path'] as String;
      if (File(p).existsSync()) return p;
      // Stale entry — purge it.
      _box.delete(videoId);
    }
    return null;
  }

  bool isDownloaded(String videoId) => localPathFor(videoId) != null;

  /// All downloaded songs (with local file path attached as `localPath`).
  List<DownloadedSong> all() {
    final list = <DownloadedSong>[];
    for (final key in _box.keys) {
      final raw = _box.get(key);
      if (raw is! Map) continue;
      final m = Map<String, dynamic>.from(raw);
      final path = m['path'] as String?;
      if (path == null || !File(path).existsSync()) continue;
      try {
        final song = Song.fromJson(Map<String, dynamic>.from(m));
        list.add(DownloadedSong(song: song, path: path,
            sizeBytes: File(path).lengthSync()));
      } catch (_) {}
    }
    return list;
  }

  Future<int> totalSizeBytes() async {
    var total = 0;
    for (final d in all()) {
      total += d.sizeBytes;
    }
    return total;
  }

  Future<File> _outFile(String videoId, int bitrate) async {
    final dir = await getApplicationDocumentsDirectory();
    final downloads = Directory('${dir.path}/downloads')
      ..createSync(recursive: true);
    return File('${downloads.path}/$videoId-${bitrate}k.mp3');
  }

  /// Download a song's MP3 via the backend. Quality is approximate —
  /// yt-dlp picks the best audio source and ffmpeg re-encodes to MP3
  /// at the requested bitrate (128 or 320).
  Future<DownloadedSong?> download(
    Song song, {
    required int bitrate,
  }) async {
    final file = await _outFile(song.videoId, bitrate);
    if (file.existsSync()) {
      _box.put(song.videoId, {
        ...song.toJson(),
        'path': file.path,
        'bitrate': bitrate,
        'downloadedAt': DateTime.now().toIso8601String(),
      });
      return DownloadedSong(
        song: song,
        path: file.path,
        sizeBytes: file.lengthSync(),
      );
    }

    final url =
        '${AppConfig.backendBaseUrl}/download/${song.videoId}?type=audio';
    _progress[song.videoId] = 0;
    _progressController.add(Map.unmodifiable(_progress));

    try {
      await _dio.download(
        url,
        file.path,
        options: Options(
          receiveTimeout: const Duration(minutes: 5),
          followRedirects: true,
          validateStatus: (c) => c != null && c < 500,
        ),
        onReceiveProgress: (rec, total) {
          if (total > 0) {
            _progress[song.videoId] = rec / total;
            _progressController.add(Map.unmodifiable(_progress));
          }
        },
      );

      if (!file.existsSync() || file.lengthSync() < 4 * 1024) {
        // Server returned an error JSON or a tiny stub — treat as failure.
        if (file.existsSync()) file.deleteSync();
        return null;
      }

      _box.put(song.videoId, {
        ...song.toJson(),
        'path': file.path,
        'bitrate': bitrate,
        'downloadedAt': DateTime.now().toIso8601String(),
      });
      return DownloadedSong(
        song: song,
        path: file.path,
        sizeBytes: file.lengthSync(),
      );
    } catch (e) {
      if (kDebugMode) debugPrint('[download] failed for ${song.videoId}: $e');
      if (file.existsSync()) file.deleteSync();
      return null;
    } finally {
      _progress.remove(song.videoId);
      _progressController.add(Map.unmodifiable(_progress));
    }
  }

  Future<void> delete(String videoId) async {
    final entry = _box.get(videoId);
    if (entry is Map && entry['path'] is String) {
      final f = File(entry['path'] as String);
      if (f.existsSync()) {
        try {
          f.deleteSync();
        } catch (_) {}
      }
    }
    await _box.delete(videoId);
  }

  Future<void> deleteAll() async {
    for (final key in _box.keys.toList()) {
      await delete(key.toString());
    }
  }

  void dispose() {
    _progressController.close();
  }
}

class DownloadedSong {
  final Song song;
  final String path;
  final int sizeBytes;

  const DownloadedSong({
    required this.song,
    required this.path,
    required this.sizeBytes,
  });
}
