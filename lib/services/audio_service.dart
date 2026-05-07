import 'dart:async';
import 'dart:math';

import 'package:audio_service/audio_service.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';

import '../models/song.dart';
import 'settings_service.dart';
import 'stream_resolver.dart';

/// Provider for the platform AudioHandler. Overridden in `main.dart`
/// after `AudioService.init(...)` returns.
final audioHandlerProvider = Provider<NocturneAudioHandler>(
  (ref) => throw UnimplementedError(
    'audioHandlerProvider must be overridden in main()',
  ),
);

/// Reactive view of the queue + current index. Used by the queue screen
/// so reorders/inserts/deletes show up live.
final queueSnapshotProvider =
    StreamProvider<({List<Song> songs, int currentIndex})>((ref) {
  final handler = ref.watch(audioHandlerProvider);
  return handler.queueChangesStream;
});

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
  final StreamController<({List<Song> songs, int currentIndex})>
      _queueController =
      StreamController<({List<Song> songs, int currentIndex})>.broadcast();
  final Random _rng = Random();
  int _currentIndex = -1;
  bool _shuffle = false;
  LoopMode _loopMode = LoopMode.off;
  List<int> _shuffleOrder = const [];

  // Crossfade / fade state.
  Duration _crossfade = Duration.zero;
  Timer? _fadeTimer;
  bool _fadingOut = false;
  StreamSubscription<Duration>? _positionSub;

  // Sleep timer.
  Timer? _sleepTimer;
  DateTime? _sleepFiresAt;
  final StreamController<DateTime?> _sleepController =
      StreamController<DateTime?>.broadcast();

  NocturneAudioHandler() {
    _player = AudioPlayer(
      audioPipeline: AudioPipeline(
        androidAudioEffects: [_loudness, _equalizer],
      ),
      // Tuned for fast start on slow connections: start playing as soon as
      // ~1s of audio is buffered (default is 2.5s) and don't pre-fetch a
      // huge buffer up front.
      audioLoadConfiguration: const AudioLoadConfiguration(
        androidLoadControl: AndroidLoadControl(
          minBufferDuration: Duration(seconds: 5),
          maxBufferDuration: Duration(seconds: 50),
          bufferForPlaybackDuration: Duration(seconds: 1),
          bufferForPlaybackAfterRebufferDuration: Duration(seconds: 3),
        ),
      ),
    );
    _player.playbackEventStream.listen(_broadcastState);
    _player.processingStateStream.listen((state) {
      if (state == ProcessingState.completed) {
        _onTrackCompleted();
      }
    });
    _positionSub = _player.positionStream.listen(_onPosition);
    // Apply persisted speed on boot.
    try {
      _player.setSpeed(SettingsService.instance.playbackSpeed);
      _crossfade = Duration(
        seconds: SettingsService.instance.crossfadeSeconds,
      );
    } catch (_) {
      // SettingsService not yet bootstrapped (tests).
    }
  }

  // ---------- public surface ----------

  AudioPlayer get player => _player;
  AndroidEqualizer get equalizer => _equalizer;
  AndroidLoudnessEnhancer get loudness => _loudness;
  List<Song> get songs => List.unmodifiable(_queue);
  int get currentIndex => _currentIndex;
  bool get shuffleEnabled => _shuffle;
  LoopMode get loopMode => _loopMode;
  Duration get crossfade => _crossfade;
  double get speed => _player.speed;
  DateTime? get sleepFiresAt => _sleepFiresAt;
  Stream<DateTime?> get sleepTimerStream => _sleepController.stream;

  /// Fires whenever the active track index changes (skip, completed,
  /// shuffle pick). Subscribed by `currentSongProvider`.
  Stream<int> get currentIndexStream => _indexController.stream;

  /// Fires whenever the queue contents OR current index change.
  Stream<({List<Song> songs, int currentIndex})> get queueChangesStream {
    return _queueController.stream;
  }

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

  /// Append a song to the end of the queue.
  Future<void> addToQueue(Song song) async {
    _queue.add(song);
    queue.add(_queue.map(_toMediaItem).toList());
    if (_shuffle) _rebuildShuffleOrder(pinFirst: _currentIndex);
    _emitQueueChanged();
  }

  /// Insert [song] right after the currently playing item.
  Future<void> playNextInQueue(Song song) async {
    if (_queue.isEmpty || _currentIndex < 0) {
      await setQueue([song]);
      return;
    }
    _queue.insert(_currentIndex + 1, song);
    queue.add(_queue.map(_toMediaItem).toList());
    if (_shuffle) _rebuildShuffleOrder(pinFirst: _currentIndex);
    _emitQueueChanged();
  }

  /// Remove a queue entry by index. If the entry being removed is the
  /// currently playing one we advance to the next.
  Future<void> removeFromQueue(int index) async {
    if (index < 0 || index >= _queue.length) return;
    final wasCurrent = index == _currentIndex;
    _queue.removeAt(index);
    if (index < _currentIndex) {
      _currentIndex--;
    }
    queue.add(_queue.map(_toMediaItem).toList());
    if (_shuffle) _rebuildShuffleOrder(pinFirst: _currentIndex);
    if (_queue.isEmpty) {
      _setIndex(-1);
      mediaItem.add(null);
      await _player.stop();
      _emitQueueChanged();
      return;
    }
    if (wasCurrent) {
      final next = _currentIndex.clamp(0, _queue.length - 1);
      await _playIndex(next);
    } else {
      _emitQueueChanged();
    }
  }

  /// Drag-reorder helper for the queue management screen.
  Future<void> reorderQueue(int oldIndex, int newIndex) async {
    if (oldIndex < 0 || oldIndex >= _queue.length) return;
    if (newIndex > oldIndex) newIndex--;
    final item = _queue.removeAt(oldIndex);
    _queue.insert(newIndex.clamp(0, _queue.length), item);
    if (_currentIndex == oldIndex) {
      _currentIndex = newIndex;
    } else if (oldIndex < _currentIndex && newIndex >= _currentIndex) {
      _currentIndex--;
    } else if (oldIndex > _currentIndex && newIndex <= _currentIndex) {
      _currentIndex++;
    }
    queue.add(_queue.map(_toMediaItem).toList());
    if (_shuffle) _rebuildShuffleOrder(pinFirst: _currentIndex);
    _emitQueueChanged();
  }

  /// Jump to a specific queue index (used by the queue UI).
  Future<void> playIndex(int index) => _playIndex(index);

  // ---------- queue navigation ----------

  Future<void> _playIndex(int index) async {
    if (index < 0 || index >= _queue.length) return;
    _cancelFade();
    _setIndex(index);
    final song = _queue[index];
    mediaItem.add(_toMediaItem(song));
    _emitQueueChanged();

    final url = await _resolveStreamUrl(song.videoId);
    if (url == null) {
      if (kDebugMode) {
        debugPrint('[audio] no stream URL resolved for ${song.videoId}');
      }
      return;
    }

    try {
      await _player.setUrl(url);
      await _player.setSpeed(SettingsService.instance.playbackSpeed);
      // Fade in if crossfade is configured.
      if (_crossfade > Duration.zero) {
        await _player.setVolume(0);
        await _player.play();
        _runVolumeFade(from: 0, to: 1, dur: _crossfade);
      } else {
        await _player.setVolume(1.0);
        await _player.play();
      }
    } catch (e) {
      if (kDebugMode) debugPrint('[audio] playback failed: $e');
      // Keep handler alive even if a single track fails to load.
    }
  }

  void _setIndex(int i) {
    _currentIndex = i;
    _indexController.add(i);
  }

  void _emitQueueChanged() {
    _queueController.add((
      songs: List<Song>.unmodifiable(_queue),
      currentIndex: _currentIndex,
    ));
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

  @override
  Future<void> skipToQueueItem(int index) async {
    await _playIndex(index);
  }

  @override
  Future<void> setSpeed(double speed) async {
    final s = speed.clamp(0.5, 2.0);
    await _player.setSpeed(s);
    await SettingsService.instance.setPlaybackSpeed(s);
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

  // ---------- Crossfade ----------

  Future<void> setCrossfade(Duration d) async {
    _crossfade = Duration(seconds: d.inSeconds.clamp(0, 12));
    await SettingsService.instance.setCrossfadeSeconds(_crossfade.inSeconds);
  }

  void _onPosition(Duration pos) {
    if (_crossfade <= Duration.zero) return;
    final dur = _player.duration;
    if (dur == null) return;
    final remaining = dur - pos;
    if (remaining <= _crossfade && !_fadingOut && _player.playing) {
      // Start fading out the current track. When fade hits 0 we advance
      // and the next track will fade in via _playIndex.
      _fadingOut = true;
      _runVolumeFade(
        from: _player.volume,
        to: 0,
        dur: remaining < _crossfade ? remaining : _crossfade,
        onComplete: () {
          _fadingOut = false;
          if (_loopMode == LoopMode.one) return;
          final next = _nextIndex();
          if (next != null) _playIndex(next);
        },
      );
    }
  }

  void _runVolumeFade({
    required double from,
    required double to,
    required Duration dur,
    VoidCallback? onComplete,
  }) {
    _cancelFade();
    if (dur <= Duration.zero) {
      _player.setVolume(to);
      onComplete?.call();
      return;
    }
    final steps = (dur.inMilliseconds / 50).clamp(1, 240).toInt();
    var step = 0;
    _player.setVolume(from);
    _fadeTimer = Timer.periodic(
      Duration(milliseconds: dur.inMilliseconds ~/ steps),
      (t) {
        step++;
        final v = (from + (to - from) * (step / steps)).clamp(0.0, 1.0);
        _player.setVolume(v);
        if (step >= steps) {
          t.cancel();
          _fadeTimer = null;
          _player.setVolume(to);
          onComplete?.call();
        }
      },
    );
  }

  void _cancelFade() {
    _fadeTimer?.cancel();
    _fadeTimer = null;
    _fadingOut = false;
  }

  // ---------- Sleep timer ----------

  void setSleepTimer(Duration? d) {
    _sleepTimer?.cancel();
    if (d == null || d <= Duration.zero) {
      _sleepTimer = null;
      _sleepFiresAt = null;
      _sleepController.add(null);
      return;
    }
    _sleepFiresAt = DateTime.now().add(d);
    _sleepController.add(_sleepFiresAt);
    _sleepTimer = Timer(d, () async {
      // Soft pause — fade out over 3s for niceness.
      _runVolumeFade(
        from: _player.volume,
        to: 0,
        dur: const Duration(seconds: 3),
        onComplete: () async {
          await _player.pause();
          await _player.setVolume(1.0);
        },
      );
      _sleepFiresAt = null;
      _sleepController.add(null);
    });
  }

  // ---------- Internals ----------

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
    _cancelFade();
    await _positionSub?.cancel();
    await _indexController.close();
    await _queueController.close();
    await _sleepController.close();
    await _player.dispose();
    _resolver.dispose();
  }
}
