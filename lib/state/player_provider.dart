import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';

import '../models/song.dart';
import '../services/api_service.dart';
import '../services/audio_service.dart';
import '../services/database_service.dart';
import '../services/settings_service.dart';

/// Currently-playing song. Listens to the handler's own index stream
/// (NOT just_audio's currentIndexStream) because we use a manual queue;
/// just_audio's index never changes since each track is loaded as a
/// fresh single source.
final currentSongProvider = StreamProvider<Song?>((ref) {
  final handler = ref.watch(audioHandlerProvider);
  return handler.currentIndexStream
      .map((_) => handler.currentSong)
      .distinct((a, b) => a?.videoId == b?.videoId);
});

/// Whether the player is actively playing.
final isPlayingProvider = StreamProvider<bool>((ref) {
  final handler = ref.watch(audioHandlerProvider);
  return handler.player.playingStream;
});

/// Whether the player is currently buffering (network fetch / decoding).
final isBufferingProvider = StreamProvider<bool>((ref) {
  final handler = ref.watch(audioHandlerProvider);
  return handler.player.processingStateStream.map(
    (s) =>
        s == ProcessingState.loading || s == ProcessingState.buffering,
  );
});

/// Current playback position.
final positionProvider = StreamProvider<Duration>((ref) {
  final handler = ref.watch(audioHandlerProvider);
  return handler.player.positionStream;
});

/// Total duration of the current track (when known).
final durationProvider = StreamProvider<Duration?>((ref) {
  final handler = ref.watch(audioHandlerProvider);
  return handler.player.durationStream;
});

/// Active queue.
final queueProvider = Provider<List<Song>>((ref) {
  final handler = ref.watch(audioHandlerProvider);
  return handler.songs;
});

/// Sleep timer expiry (or null when no timer is set).
final sleepTimerProvider = StreamProvider<DateTime?>((ref) {
  final handler = ref.watch(audioHandlerProvider);
  return handler.sleepTimerStream;
});

/// Convenience high-level controller exposed to the UI.
final playerControllerProvider =
    Provider<PlayerController>((ref) => PlayerController(ref));

class PlayerController {
  PlayerController(this._ref);

  final Ref _ref;

  NocturneAudioHandler get _handler => _ref.read(audioHandlerProvider);
  DatabaseService get _db => _ref.read(databaseServiceProvider);
  ApiService get _api => _ref.read(apiServiceProvider);

  Future<void> playSong(Song song, {List<Song>? queue}) async {
    final list = queue ?? [song];
    final index = list.indexWhere((s) => s.videoId == song.videoId);
    await _handler.setQueue(list, startIndex: index < 0 ? 0 : index);
    await _db.markPlayed(song);
  }

  Future<void> playQueue(List<Song> songs, {int startIndex = 0}) async {
    await _handler.setQueue(songs, startIndex: startIndex);
    if (songs.isNotEmpty) {
      final i = startIndex.clamp(0, songs.length - 1);
      await _db.markPlayed(songs[i]);
    }
  }

  /// Smart shuffle: builds a fresh similar-songs queue around [seed].
  Future<int> smartShuffle(Song seed) async {
    final list = await _api.similarTo(seed);
    if (list.isEmpty) return 0;
    await _handler.setQueue(list, startIndex: 0);
    await _db.markPlayed(list.first);
    return list.length;
  }

  Future<void> togglePlay() async {
    if (_handler.player.playing) {
      await _handler.pause();
    } else {
      await _handler.play();
    }
  }

  Future<void> seek(Duration p) => _handler.seek(p);
  Future<void> next() => _handler.skipToNext();
  Future<void> previous() => _handler.skipToPrevious();
  Future<void> setShuffle(bool enabled) => _handler.toggleShuffle(enabled);
  Future<void> setRepeat(LoopMode mode) => _handler.setLoopMode(mode);
  Future<void> setSpeed(double s) => _handler.setSpeed(s);
  Future<void> setCrossfade(Duration d) => _handler.setCrossfade(d);
  void setSleepTimer(Duration? d) => _handler.setSleepTimer(d);
  Future<void> addToQueue(Song s) => _handler.addToQueue(s);
  Future<void> playNext(Song s) => _handler.playNextInQueue(s);
  Future<void> removeFromQueue(int i) => _handler.removeFromQueue(i);
  Future<void> reorderQueue(int from, int to) =>
      _handler.reorderQueue(from, to);
  Future<void> jumpToQueueIndex(int i) => _handler.playIndex(i);

  /// Persist current song + position so the next launch can offer
  /// "continue where left off". Best-effort.
  Future<void> snapshotForResume() async {
    final s = _handler.currentSong;
    if (s == null) {
      await SettingsService.instance.snapshot(null, Duration.zero);
      return;
    }
    await SettingsService.instance.snapshot(
      s,
      _handler.player.position,
    );
  }
}
