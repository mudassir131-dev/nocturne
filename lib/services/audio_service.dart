import 'dart:async';
import 'dart:math';

import 'package:audio_service/audio_service.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';

import '../models/song.dart';
import 'stream_resolver.dart';

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
///
/// Owns a manual queue (since we resolve stream URLs lazily — see
/// [StreamResolver]) and exposes its own [currentIndexStream] so the UI
/// stays in sync when shuffle / auto-advance fire.
class NocturneAudioHandler extends BaseAudioHandler with SeekHandler {
  final AndroidEqualizer _equalizer = AndroidEqualizer();
  final AndroidLoudnessEnhancer _loudness = AndroidLoudnessEnhancer();
  late final AudioPlayer _player;
  final StreamResolver _resolver = StreamResolver();
  final List<Song> _queue = [];
  final StreamController<int> _indexController =
      StreamController<int>.broadcast();
  final Random _rng = Random();
  int _currentIndex = -1;
  bool _shuffle = false;
  LoopMode _loopMode = LoopMode.off;
  List<int> _shuffleOrder = const [];

  NocturneAudioHandler() {
    _player = AudioPlayer(
      audioPipeline: AudioPipeline(
        androidAudioEffects: [_loudness, _equalizer],
      ),
    );
    _player.playbackEventStream.listen(_broadcastState);
    _player.processingStateStream.listen((state) {
      if (state == ProcessingState.completed) {
        _onTrackCompleted();
      }
    });
  }

  // ---------- public surface ----------

  AudioPlayer get player => _player;
  AndroidEqualizer get equalizer => _equalizer;
  AndroidLoudnessEnhancer get loudness => _loudness;
  List<Song> get songs => List.unmodifiable(_queue);
  int get currentIndex => _currentIndex;
  bool get shuffleEnabled => _shuffle;
  LoopMode get loopMode => _loopMode;

  /// Fires whenever the active track index changes (skip, completed,
  /// shuffle pick). Subscribed by `currentSongProvider`.
  Stream<int> get currentIndexStream => _indexController.stream;

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
    if (_shuffle) _rebuildShuffleOrder(pinFirst: startIndex);
    if (songs.isEmpty) {
      _setIndex(-1);
      mediaItem.add(null);
      await _player.stop();
      return;
    }
    await _playIndex(startIndex.clamp(0, songs.length - 1));
  }

  // ---------- queue navigation ----------

  Future<void> _playIndex(int index) async {
    if (index < 0 || index >= _queue.length) return;
    _setIndex(index);
    final song = _queue[index];
    mediaItem.add(_toMediaItem(song));

    final url = await _resolveStreamUrl(song.videoId);
    if (url == null) {
      if (kDebugMode) {
        debugPrint('[audio] no stream URL resolved for ${song.videoId}');
      }
      return;
    }

    try {
      await _player.setUrl(url);
      await _player.play();
    } catch (e) {
      if (kDebugMode) debugPrint('[audio] playback failed: $e');
      // Keep handler alive even if a single track fails to load.
    }
  }

  void _setIndex(int i) {
    _currentIndex = i;
    _indexController.add(i);
  }

  Future<String?> _resolveStreamUrl(String videoId) =>
      _resolver.resolve(videoId);

  void _onTrackCompleted() {
    if (_loopMode == LoopMode.one) {
      // Replay current track from the start.
      _player.seek(Duration.zero);
      _player.play();
      return;
    }
    if (_queue.isEmpty) return;
    final next = _nextIndex();
    if (next == null) {
      // End of queue with LoopMode.off: stop without advancing.
      return;
    }
    _playIndex(next);
  }

  /// Returns the next index to play, honoring shuffle + loopMode.
  /// `null` means "stop" (only happens when LoopMode.off and we've
  /// reached the natural end).
  int? _nextIndex() {
    if (_queue.isEmpty) return null;
    if (_shuffle) {
      // Maintain a randomized order list; advance through it cyclically.
      if (_shuffleOrder.isEmpty || _shuffleOrder.length != _queue.length) {
        _rebuildShuffleOrder(pinFirst: _currentIndex);
      }
      final pos = _shuffleOrder.indexOf(_currentIndex);
      if (pos == -1) return _shuffleOrder.first;
      if (pos + 1 < _shuffleOrder.length) return _shuffleOrder[pos + 1];
      // Reached end of shuffle order.
      if (_loopMode == LoopMode.all) {
        _rebuildShuffleOrder(pinFirst: -1);
        return _shuffleOrder.isEmpty ? null : _shuffleOrder.first;
      }
      return null;
    }
    if (_currentIndex + 1 < _queue.length) return _currentIndex + 1;
    if (_loopMode == LoopMode.all) return 0;
    return null;
  }

  int? _previousIndex() {
    if (_queue.isEmpty) return null;
    if (_shuffle) {
      final pos = _shuffleOrder.indexOf(_currentIndex);
      if (pos > 0) return _shuffleOrder[pos - 1];
      return _shuffleOrder.isEmpty ? null : _shuffleOrder.last;
    }
    if (_currentIndex - 1 >= 0) return _currentIndex - 1;
    return _queue.length - 1;
  }

  void _rebuildShuffleOrder({required int pinFirst}) {
    final indices = List<int>.generate(_queue.length, (i) => i);
    indices.shuffle(_rng);
    if (pinFirst >= 0 && pinFirst < indices.length) {
      indices.remove(pinFirst);
      indices.insert(0, pinFirst);
    }
    _shuffleOrder = indices;
  }

  MediaItem _toMediaItem(Song song) => MediaItem(
        id: song.videoId,
        title: song.title,
        artist: song.artist,
        artUri: song.thumbnail.isNotEmpty ? Uri.tryParse(song.thumbnail) : null,
        duration: song.duration,
      );

  // ---------- AudioHandler overrides ----------

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
    final n = _nextIndex();
    if (n == null) return;
    await _playIndex(n);
  }

  @override
  Future<void> skipToPrevious() async {
    final p = _previousIndex();
    if (p == null) return;
    await _playIndex(p);
  }

  Future<void> toggleShuffle(bool enabled) async {
    _shuffle = enabled;
    if (enabled) {
      _rebuildShuffleOrder(pinFirst: _currentIndex);
    } else {
      _shuffleOrder = const [];
    }
    // Echo into AudioService PlaybackState so the system UI / lock screen
    // shuffle indicator (where rendered) stays in sync.
    playbackState.add(playbackState.value.copyWith(
      shuffleMode:
          enabled ? AudioServiceShuffleMode.all : AudioServiceShuffleMode.none,
    ));
  }

  /// Custom helper (not the AudioService override). Tracks loop mode
  /// internally so the auto-advance handler can honour it.
  Future<void> setLoopMode(LoopMode mode) async {
    _loopMode = mode;
    final repeat = switch (mode) {
      LoopMode.off => AudioServiceRepeatMode.none,
      LoopMode.one => AudioServiceRepeatMode.one,
      LoopMode.all => AudioServiceRepeatMode.all,
    };
    playbackState.add(playbackState.value.copyWith(repeatMode: repeat));
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

  Future<void> dispose() async {
    await _indexController.close();
    await _player.dispose();
    _resolver.dispose();
  }
}
