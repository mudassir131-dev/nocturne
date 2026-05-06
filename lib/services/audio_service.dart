import 'package:audio_service/audio_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';

import '../models/song.dart';
import '../utils/config.dart';

/// Provider for the platform AudioHandler. Overridden in `main.dart`
/// after `AudioService.init(...)` returns.
final audioHandlerProvider = Provider<NocturneAudioHandler>(
  (ref) => throw UnimplementedError(
    'audioHandlerProvider must be overridden in main()',
  ),
);

/// Background audio handler bridging just_audio with audio_service so
/// playback continues when the app is backgrounded and is controllable
/// from the lock screen / notification shade.
class NocturneAudioHandler extends BaseAudioHandler with SeekHandler {
  final AudioPlayer _player = AudioPlayer();
  final List<Song> _queue = [];
  int _currentIndex = -1;

  NocturneAudioHandler() {
    _player.playbackEventStream.listen(_broadcastState);
    _player.processingStateStream.listen((state) {
      if (state == ProcessingState.completed) {
        skipToNext();
      }
    });
  }

  AudioPlayer get player => _player;
  List<Song> get songs => List.unmodifiable(_queue);
  int get currentIndex => _currentIndex;
  Song? get currentSong =>
      (_currentIndex >= 0 && _currentIndex < _queue.length)
          ? _queue[_currentIndex]
          : null;

  /// Replace the queue with [songs] and start playback at [startIndex].
  Future<void> setQueue(List<Song> songs, {int startIndex = 0}) async {
    _queue
      ..clear()
      ..addAll(songs);
    queue.add(_queue.map(_toMediaItem).toList());
    if (songs.isEmpty) {
      _currentIndex = -1;
      mediaItem.add(null);
      await _player.stop();
      return;
    }
    await _playIndex(startIndex.clamp(0, songs.length - 1));
  }

  Future<void> _playIndex(int index) async {
    if (index < 0 || index >= _queue.length) return;
    _currentIndex = index;
    final song = _queue[index];
    mediaItem.add(_toMediaItem(song));
    final url = '${AppConfig.backendBaseUrl}/stream/${song.videoId}';
    try {
      await _player.setUrl(url);
      await _player.play();
    } catch (_) {
      // Keep handler alive even if a single track fails to load.
    }
  }

  MediaItem _toMediaItem(Song song) => MediaItem(
        id: song.videoId,
        title: song.title,
        artist: song.artist,
        artUri: song.thumbnail.isNotEmpty ? Uri.tryParse(song.thumbnail) : null,
        duration: song.duration,
      );

  @override
  Future<void> play() => _player.play();

  @override
  Future<void> pause() => _player.pause();

  @override
  Future<void> stop() async {
    await _player.stop();
    return super.stop();
  }

  @override
  Future<void> seek(Duration position) => _player.seek(position);

  @override
  Future<void> skipToNext() async {
    if (_queue.isEmpty) return;
    final next = (_currentIndex + 1) % _queue.length;
    await _playIndex(next);
  }

  @override
  Future<void> skipToPrevious() async {
    if (_queue.isEmpty) return;
    final prev = (_currentIndex - 1) < 0
        ? _queue.length - 1
        : _currentIndex - 1;
    await _playIndex(prev);
  }

  Future<void> toggleShuffle(bool enabled) async {
    await _player.setShuffleModeEnabled(enabled);
  }

  /// Custom helper (not the AudioService override). Uses just_audio's
  /// LoopMode directly since the UI controls map cleanly onto it.
  Future<void> setLoopMode(LoopMode mode) async {
    await _player.setLoopMode(mode);
  }

  void _broadcastState(PlaybackEvent event) {
    final playing = _player.playing;
    playbackState.add(playbackState.value.copyWith(
      controls: [
        MediaControl.skipToPrevious,
        if (playing) MediaControl.pause else MediaControl.play,
        MediaControl.skipToNext,
      ],
      systemActions: const {
        MediaAction.seek,
        MediaAction.seekForward,
        MediaAction.seekBackward,
      },
      androidCompactActionIndices: const [0, 1, 2],
      processingState: const {
        ProcessingState.idle: AudioProcessingState.idle,
        ProcessingState.loading: AudioProcessingState.loading,
        ProcessingState.buffering: AudioProcessingState.buffering,
        ProcessingState.ready: AudioProcessingState.ready,
        ProcessingState.completed: AudioProcessingState.completed,
      }[_player.processingState]!,
      playing: playing,
      updatePosition: _player.position,
      bufferedPosition: _player.bufferedPosition,
      speed: _player.speed,
      queueIndex: _currentIndex >= 0 ? _currentIndex : null,
    ));
  }
}
