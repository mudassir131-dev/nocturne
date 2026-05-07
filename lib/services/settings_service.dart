import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/song.dart';

/// Persistent user preferences (playback speed, crossfade, last-played
/// snapshot for "continue where left off", etc).
///
/// Backed by [SharedPreferences]. Hot-loaded once at bootstrap so the
/// rest of the app can read synchronously.
final settingsServiceProvider =
    Provider<SettingsService>((_) => SettingsService.instance);

class SettingsService {
  SettingsService._(this._prefs);

  static late final SettingsService instance;

  final SharedPreferences _prefs;

  static const String _kSpeed = 'playback_speed';
  static const String _kCrossfade = 'crossfade_seconds';
  static const String _kDownloadQuality = 'download_quality_kbps';
  static const String _kLastSong = 'last_song_json';
  static const String _kLastPosition = 'last_position_ms';

  /// Must be called once before runApp.
  static Future<void> bootstrap() async {
    final prefs = await SharedPreferences.getInstance();
    instance = SettingsService._(prefs);
  }

  // ---------------- Playback speed ----------------

  double get playbackSpeed =>
      (_prefs.getDouble(_kSpeed) ?? 1.0).clamp(0.5, 2.0);

  Future<void> setPlaybackSpeed(double v) async {
    await _prefs.setDouble(_kSpeed, v.clamp(0.5, 2.0));
  }

  // ---------------- Crossfade ----------------

  /// 0..12 seconds. 0 disables crossfade.
  int get crossfadeSeconds => _prefs.getInt(_kCrossfade) ?? 0;

  Future<void> setCrossfadeSeconds(int v) async {
    await _prefs.setInt(_kCrossfade, v.clamp(0, 12));
  }

  // ---------------- Download quality ----------------

  /// 128 or 320.
  int get downloadQualityKbps => _prefs.getInt(_kDownloadQuality) ?? 320;

  Future<void> setDownloadQualityKbps(int v) async {
    await _prefs.setInt(_kDownloadQuality, v == 128 ? 128 : 320);
  }

  // ---------------- Continue where left off ----------------

  Song? get lastSong {
    final raw = _prefs.getString(_kLastSong);
    if (raw == null || raw.isEmpty) return null;
    try {
      final m = jsonDecode(raw);
      if (m is Map) return Song.fromJson(Map<String, dynamic>.from(m));
    } catch (_) {}
    return null;
  }

  Duration get lastPosition =>
      Duration(milliseconds: _prefs.getInt(_kLastPosition) ?? 0);

  Future<void> snapshot(Song? song, Duration position) async {
    if (song == null) {
      await _prefs.remove(_kLastSong);
      await _prefs.remove(_kLastPosition);
      return;
    }
    await _prefs.setString(_kLastSong, jsonEncode(song.toJson()));
    await _prefs.setInt(_kLastPosition, position.inMilliseconds);
  }
}
