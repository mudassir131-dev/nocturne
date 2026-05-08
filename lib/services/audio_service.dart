import 'dart:async';
import 'dart:math';

import 'package:audio_service/audio_service.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';

import '../models/song.dart';
import '../utils/config.dart';
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
  final StreamController<int> _queueRevisionController =
      StreamController<int>.broadcast();
  final StreamController<Duration?> _sleepController =
      StreamController<Duration?>.broadcast();
  final Random _rng = Random();
  int _currentIndex = -1;
  int _queueRevision = 0;
  bool _shuffle = false;
  LoopMode _loopMode = LoopMode.off;
  List<int> _shuffleOrder = const [];
  Timer? _sleepTimer;
  Timer? _sleepTickTimer;
  Duration? _sleepRemaining;
  double _crossfadeSeconds = 0;
  bool _gapless = true;
  Timer? _crossfadeTimer;

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

  /// Fires whenever the queue contents (not just the index) change. The
  /// emitted int is a monotonically increasing revision so listeners can
  /// `.distinct()` cheaply.
  Stream<int> get queueRevisionStream => _queueRevisionController.stream;

  /// Active sleep-timer remaining duration (null when no timer is set).
  Duration? get sleepRemaining => _sleepRemaining;

  /// Live-updating sleep-timer countdown for the player UI.
  Stream<Duration?> get sleepRemainingStream => _sleepController.stream;

  /// Crossfade tail duration in seconds (0 disables it).
  double get crossfadeSeconds => _crossfadeSeconds;

  /// Whether gapless transitions are enabled. When `true` we pre-resolve
  /// the next track's stream URL while the current one is still playing,
  /// trimming the gap between songs to whatever the network allows.
  bool get gapless => _gapless;

  /// Current playback speed (0.5x – 2.0x).
  double get speed => _player.speed;

  Song? get currentSong => (_currentIndex >= 0 && _currentIndex < _queue.length)
      ? _queue[_currentIndex]
      : null;

  /// Replace the queue with [songs] and start playback at [startIndex].
  Future<void> setQueue(List<Song> songs, {int startIndex = 0}) async {
    _queue
      ..clear()
      ..addAll(songs);
    _bumpQueueRevision();
    if (_shuffle) _rebuildShuffleOrder(pinFirst: startIndex);
    if (songs.isEmpty) {
      _setIndex(-1);
      mediaItem.add(null);
      await _player.stop();
      return;
    }
    await _playIndex(startIndex.clamp(0, songs.length - 1));
  }

  /// Jump the queue to a specific [index] without reshuffling.
  Future<void> playIndex(int index) async {
    if (index < 0 || index >= _queue.length) return;
    await _playIndex(index);
  }

  /// Reorder the queue so the song at [oldIndex] moves to [newIndex].
  /// Both indices are global (queue-relative). Tracks the active item so
  /// the currently-playing track keeps highlighting correctly.
  void reorder(int oldIndex, int newIndex) {
    if (oldIndex == newIndex) return;
    if (oldIndex < 0 || oldIndex >= _queue.length) return;
    if (newIndex < 0 || newIndex >= _queue.length) return;
    final song = _queue.removeAt(oldIndex);
    _queue.insert(newIndex, song);
    if (_currentIndex == oldIndex) {
      _setIndex(newIndex);
    } else {
      // Adjust the active index as items slide past it.
      if (oldIndex < _currentIndex && newIndex >= _currentIndex) {
        _setIndex(_currentIndex - 1);
      } else if (oldIndex > _currentIndex && newIndex <= _currentIndex) {
        _setIndex(_currentIndex + 1);
      }
    }
    if (_shuffle) _rebuildShuffleOrder(pinFirst: _currentIndex);
    _bumpQueueRevision();
  }

  /// Remove the queue entry at [index]. Refuses to remove the actively
  /// playing track (callers should `skipToNext` first).
  void removeAt(int index) {
    if (index < 0 || index >= _queue.length) return;
    if (index == _currentIndex) return;
    _queue.removeAt(index);
    if (index < _currentIndex) {
      _setIndex(_currentIndex - 1);
    }
    if (_shuffle) _rebuildShuffleOrder(pinFirst: _currentIndex);
    _bumpQueueRevision();
  }

  void _bumpQueueRevision() {
    queue.add(_queue.map(_toMediaItem).toList());
    _queueRevision += 1;
    _queueRevisionController.add(_queueRevision);
  }

  /// Schedule the player to pause after [duration]. Pass `null` to clear.
  /// Setting a duration starts a 1-second tick that streams the remaining
  /// time on [sleepRemainingStream] for display in the player UI.
  void setSleepTimer(Duration? duration) {
    _sleepTimer?.cancel();
    _sleepTickTimer?.cancel();
    _sleepTimer = null;
    _sleepTickTimer = null;
    _sleepRemaining = duration;
    _sleepController.add(_sleepRemaining);
    if (duration == null) return;
    final endsAt = DateTime.now().add(duration);
    _sleepTimer = Timer(duration, () {
      _sleepRemaining = null;
      _sleepController.add(null);
      _sleepTickTimer?.cancel();
      _player.pause();
    });
    _sleepTickTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      final remaining = endsAt.difference(DateTime.now());
      if (remaining <= Duration.zero) {
        _sleepRemaining = null;
        _sleepController.add(null);
        _sleepTickTimer?.cancel();
      } else {
        _sleepRemaining = remaining;
        _sleepController.add(remaining);
      }
    });
  }

  /// Update the crossfade tail in seconds (clamped 0–12).
  void setCrossfadeSeconds(double seconds) {
    _crossfadeSeconds = seconds.clamp(0, 12);
  }

  /// Toggle gapless playback (next-track pre-resolution).
  void setGapless(bool enabled) {
    _gapless = enabled;
  }

  /// Update playback speed (0.5x–2.0x).
  Future<void> setSpeed(double speed) =>
      _player.setSpeed(speed.clamp(0.5, 2.0));

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
      // Use setAudioSource so we can attach an Authorization header when
      // the backend sits behind a basic-auth tunnel.
      final headers = AppConfig.streamHeaders;
      if (headers.isEmpty) {
        await _player.setUrl(url);
      } else {
        await _player.setAudioSource(
          AudioSource.uri(Uri.parse(url), headers: headers),
        );
      }
      await _player.play();
      _scheduleCrossfade();
      _prefetchNext();
    } catch (e) {
      if (kDebugMode) debugPrint('[audio] playback failed: $e');
      // Keep handler alive even if a single track fails to load.
    }
  }

  /// Pre-resolve the next track's stream URL so the gap when the current
  /// track ends is just the time it takes just_audio to swap sources.
  void _prefetchNext() {
    if (!_gapless) return;
    final next = _peekNextIndex();
    if (next == null || next < 0 || next >= _queue.length) return;
    final song = _queue[next];
    // Fire-and-forget; resolver caches internally.
    _resolver.resolve(song.videoId);
  }

  int? _peekNextIndex() {
    if (_queue.isEmpty) return null;
    if (_shuffle) {
      if (_shuffleOrder.isEmpty) return null;
      final pos = _shuffleOrder.indexOf(_currentIndex);
      if (pos == -1) return null;
      if (pos + 1 < _shuffleOrder.length) return _shuffleOrder[pos + 1];
      if (_loopMode == LoopMode.all) return _shuffleOrder.first;
      return null;
    }
    if (_currentIndex + 1 < _queue.length) return _currentIndex + 1;
    if (_loopMode == LoopMode.all) return 0;
    return null;
  }

  /// Fade-out scheduling for crossfade. We watch the position stream and
  /// when we cross into the last [_crossfadeSeconds] of the current track
  /// we ramp the volume down so the transition feels smooth, even though
  /// the actual swap still happens via `_onTrackCompleted`.
  void _scheduleCrossfade() {
    _crossfadeTimer?.cancel();
    if (_crossfadeSeconds <= 0) return;
    final dur = _player.duration;
    if (dur == null) return;
    final tail = Duration(milliseconds: (_crossfadeSeconds * 1000).round());
    final fadeStart = dur - tail;
    if (fadeStart <= Duration.zero) return;
    _crossfadeTimer = Timer(
      fadeStart - _player.position,
      () async {
        try {
          // Linear fade from current volume to 0 over the tail.
          final start = _player.volume;
          const steps = 20;
          final stepDur = Duration(
            milliseconds: (tail.inMilliseconds / steps).round(),
          );
          for (var i = 1; i <= steps; i++) {
            await Future<void>.delayed(stepDur);
            await _player.setVolume(start * (1 - i / steps));
          }
          // Restore volume for the next track once it's loaded.
          await _player.setVolume(start);
        } catch (_) {/* ignore */}
      },
    );
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
    _sleepTimer?.cancel();
    _sleepTickTimer?.cancel();
    _crossfadeTimer?.cancel();
    await _sleepController.close();
    await _indexController.close();
    await _queueRevisionController.close();
    await _player.dispose();
    _resolver.dispose();
  }
}
