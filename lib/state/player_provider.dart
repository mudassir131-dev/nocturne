import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';

import '../models/song.dart';
import '../services/audio_service.dart';
import '../services/database_service.dart';

/// Currently-playing song. Null when nothing is loaded.
final currentSongProvider = StreamProvider<Song?>((ref) {
  final handler = ref.watch(audioHandlerProvider);
  return handler.player.currentIndexStream.map(
    (_) => handler.currentSong,
  );
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
}
