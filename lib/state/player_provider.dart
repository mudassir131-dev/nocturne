import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';

import '../models/song.dart';
import '../services/audio_service.dart';
import '../services/database_service.dart';

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

/// Emits whenever the queue contents change (reorder, remove, replace).
/// Useful for widgets that need to rebuild on structural queue mutations
/// without subscribing to the heavier stream of media item updates.
final queueRevisionProvider = StreamProvider<int>((ref) {
  final handler = ref.watch(audioHandlerProvider);
  return handler.queueRevisionStream;
});

/// Convenience high-level controller exposed to the UI.
final playerControllerProvider =
    Provider<PlayerController>((ref) => PlayerController(ref));

class PlayerController {
  PlayerController(this._ref);

  final Ref _ref;

  NocturneAudioHandler get _handler => _ref.read(audioHandlerProvider);
  DatabaseService get _db => _ref.read(databaseServiceProvider);

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
  Future<void> jumpTo(int index) => _handler.playIndex(index);
  void reorder(int oldIndex, int newIndex) =>
      _handler.reorder(oldIndex, newIndex);
  void removeAt(int index) => _handler.removeAt(index);
  void setSleepTimer(Duration? d) => _handler.setSleepTimer(d);
  void setCrossfadeSeconds(double s) => _handler.setCrossfadeSeconds(s);
  Future<void> setSpeed(double s) => _handler.setSpeed(s);
}

/// Live sleep-timer countdown (null = no timer set).
final sleepRemainingProvider = StreamProvider<Duration?>((ref) {
  final handler = ref.watch(audioHandlerProvider);
  return handler.sleepRemainingStream;
});

/// Active playback speed.
final playbackSpeedProvider = StreamProvider<double>((ref) {
  final handler = ref.watch(audioHandlerProvider);
  return handler.player.speedStream;
});
