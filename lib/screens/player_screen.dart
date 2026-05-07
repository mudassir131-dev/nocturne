import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';
import 'package:palette_generator/palette_generator.dart';
import 'package:share_plus/share_plus.dart';

import '../models/song.dart';
import '../services/audio_service.dart';
import '../services/database_service.dart';
import '../services/settings_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import 'equalizer_screen.dart';
import 'lyrics_screen.dart';
import 'queue_screen.dart';
import 'sleep_timer_screen.dart';

/// Full-screen player that exactly mirrors the supplied Apple-Music-style
/// design:
///   - dynamic dominant-color gradient (lighter top → darker bottom),
///     smoothly tweened on track change
///   - large rounded album art (top ~half), subtle shadow
///   - title + artist on the left; favourite + overflow on the right
///   - draggable full-width progress bar with time labels underneath
///   - simple white icon controls (previous / play-pause / next, no
///     circular play button)
///   - bottom row: Lyrics (chat bubble) left, Queue (list) right
///   - shuffle / repeat / speed / sleep / crossfade / share / equalizer
///     are all reachable via the overflow menu (so the main surface stays
///     uncluttered, but no feature is lost)
///   - swipe-down to dismiss back to the mini player
class PlayerScreen extends ConsumerStatefulWidget {
  const PlayerScreen({super.key});

  @override
  ConsumerState<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends ConsumerState<PlayerScreen> {
  Color _bg = const Color(0xFF222222);
  Color _bgPrev = const Color(0xFF222222);
  String? _lastArt;

  Future<void> _refreshPalette(String? artUrl) async {
    if (artUrl == null || artUrl.isEmpty || artUrl == _lastArt) return;
    _lastArt = artUrl;
    try {
      final palette = await PaletteGenerator.fromImageProvider(
        CachedNetworkImageProvider(artUrl),
        size: const Size(220, 220),
      );
      final c = palette.dominantColor?.color ??
          palette.vibrantColor?.color ??
          palette.mutedColor?.color;
      if (!mounted || c == null) return;
      setState(() {
        _bgPrev = _bg;
        _bg = c;
      });
    } catch (_) {
      // Ignore palette failures and keep the previous background.
    }
  }

  @override
  Widget build(BuildContext context) {
    final song = ref.watch(currentSongProvider).value;
    if (song == null) {
      return const Scaffold(
        backgroundColor: Colors.black,
        body: Center(
          child: Text(
            'Nothing playing',
            style: TextStyle(color: Colors.white60),
          ),
        ),
      );
    }

    _refreshPalette(song.thumbnail);

    return TweenAnimationBuilder<Color?>(
      tween: ColorTween(begin: _bgPrev, end: _bg),
      duration: const Duration(milliseconds: 800),
      curve: Curves.easeInOut,
      builder: (context, value, _) {
        final base = value ?? _bg;
        // Top: lighter; bottom: darker.
        final top = _shift(base, 0.18);
        final mid = _shift(base, -0.05);
        final bottom = _shift(base, -0.45);
        return AnnotatedRegion<SystemUiOverlayStyle>(
          value: const SystemUiOverlayStyle(
            statusBarIconBrightness: Brightness.light,
            statusBarColor: Colors.transparent,
          ),
          child: GestureDetector(
            onVerticalDragEnd: (d) {
              if ((d.primaryVelocity ?? 0) > 250) {
                Navigator.of(context).maybePop();
              }
            },
            child: Scaffold(
              backgroundColor: base,
              body: Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [top, mid, bottom],
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    stops: const [0.0, 0.55, 1.0],
                  ),
                ),
                child: SafeArea(
                  child: Column(
                    children: [
                      _TopBar(),
                      const SizedBox(height: 12),
                      Expanded(
                        flex: 5,
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 28),
                          child: _Artwork(song: song),
                        ),
                      ),
                      const SizedBox(height: 24),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 28),
                        child: _TitleRow(song: song),
                      ),
                      const SizedBox(height: 18),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 24),
                        child: _ProgressSection(),
                      ),
                      const SizedBox(height: 12),
                      _Controls(),
                      const Spacer(),
                      Padding(
                        padding: const EdgeInsets.fromLTRB(28, 0, 28, 18),
                        child: _BottomRow(song: song),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  /// Shifts an HSL lightness by [delta] (clamped 0..1) — used to derive
  /// the top/bottom gradient stops from the dominant album-art color.
  static Color _shift(Color c, double delta) {
    final hsl = HSLColor.fromColor(c);
    final l = (hsl.lightness + delta).clamp(0.05, 0.95);
    return hsl.withLightness(l).toColor();
  }
}

class _TopBar extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 4, 8, 0),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.keyboard_arrow_down,
                color: Colors.white, size: 32),
            onPressed: () => Navigator.of(context).maybePop(),
          ),
          const Spacer(),
          const Text(
            'Now Playing',
            style: TextStyle(
              color: Colors.white,
              fontSize: 13,
              fontWeight: FontWeight.w600,
              letterSpacing: 0.6,
            ),
          ),
          const Spacer(),
          const SizedBox(width: 48),
        ],
      ),
    );
  }
}

class _Artwork extends StatelessWidget {
  final Song song;
  const _Artwork({required this.song});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Hero(
        tag: 'player-art-${song.videoId}',
        child: AspectRatio(
          aspectRatio: 1,
          child: ClipRRect(
            borderRadius: BorderRadius.circular(16),
            child: DecoratedBox(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(16),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.45),
                    blurRadius: 30,
                    spreadRadius: 4,
                    offset: const Offset(0, 12),
                  ),
                ],
              ),
              child: song.thumbnail.isEmpty
                  ? Container(
                      color: Colors.black26,
                      child: const Icon(Icons.music_note,
                          color: Colors.white54, size: 80),
                    )
                  : CachedNetworkImage(
                      imageUrl: song.thumbnail,
                      fit: BoxFit.cover,
                      memCacheWidth: 720,
                      memCacheHeight: 720,
                      placeholder: (_, __) =>
                          Container(color: Colors.black26),
                      errorWidget: (_, __, ___) => Container(
                        color: Colors.black26,
                        child: const Icon(Icons.broken_image,
                            color: Colors.white54, size: 64),
                      ),
                    ),
            ),
          ),
        ),
      ),
    );
  }
}

class _TitleRow extends ConsumerWidget {
  final Song song;
  const _TitleRow({required this.song});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final db = ref.watch(databaseServiceProvider);
    final liked = db.isLiked(song.videoId);
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                song.title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w800,
                  fontSize: 22,
                  letterSpacing: -0.2,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                song.artist,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: Colors.white.withOpacity(0.72),
                  fontSize: 15,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
        IconButton(
          icon: Icon(
            liked ? Icons.star_rounded : Icons.star_outline_rounded,
            color: liked ? Colors.amberAccent : Colors.white,
            size: 28,
          ),
          onPressed: () async {
            HapticFeedback.selectionClick();
            if (liked) {
              await db.unlikeSong(song.videoId);
            } else {
              await db.likeSong(song);
            }
            (context as Element).markNeedsBuild();
          },
        ),
        IconButton(
          icon: const Icon(Icons.more_horiz, color: Colors.white, size: 28),
          onPressed: () => _showOverflow(context, ref, song),
        ),
      ],
    );
  }
}

class _ProgressSection extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final pos = ref.watch(positionProvider).value ?? Duration.zero;
    final dur = ref.watch(durationProvider).value ?? Duration.zero;
    final total = dur.inMilliseconds;
    final value = total > 0
        ? (pos.inMilliseconds.clamp(0, total)).toDouble()
        : 0.0;
    return Column(
      children: [
        SliderTheme(
          data: SliderTheme.of(context).copyWith(
            trackHeight: 3,
            activeTrackColor: Colors.white,
            inactiveTrackColor: Colors.white.withOpacity(0.25),
            thumbColor: Colors.white,
            thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
            overlayShape: const RoundSliderOverlayShape(overlayRadius: 14),
            overlayColor: Colors.white.withOpacity(0.18),
          ),
          child: Slider(
            value: value,
            min: 0,
            max: total > 0 ? total.toDouble() : 1,
            onChanged: (v) {
              ref
                  .read(playerControllerProvider)
                  .seek(Duration(milliseconds: v.toInt()));
            },
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                _fmt(pos),
                style: TextStyle(
                  color: Colors.white.withOpacity(0.8),
                  fontSize: 12,
                  fontFeatures: const [FontFeature.tabularFigures()],
                ),
              ),
              Text(
                '-${_fmt(dur - pos)}',
                style: TextStyle(
                  color: Colors.white.withOpacity(0.8),
                  fontSize: 12,
                  fontFeatures: const [FontFeature.tabularFigures()],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  static String _fmt(Duration d) {
    if (d.isNegative) d = Duration.zero;
    final m = d.inMinutes.toString();
    final s = (d.inSeconds % 60).toString().padLeft(2, '0');
    return '$m:$s';
  }
}

class _Controls extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final playing = ref.watch(isPlayingProvider).value ?? false;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          _IconBtn(
            icon: Icons.skip_previous_rounded,
            size: 44,
            onTap: () => ref.read(playerControllerProvider).previous(),
          ),
          _PlayPause(playing: playing),
          _IconBtn(
            icon: Icons.skip_next_rounded,
            size: 44,
            onTap: () => ref.read(playerControllerProvider).next(),
          ),
        ],
      ),
    );
  }
}

class _IconBtn extends StatefulWidget {
  final IconData icon;
  final double size;
  final VoidCallback onTap;

  const _IconBtn({required this.icon, required this.size, required this.onTap});

  @override
  State<_IconBtn> createState() => _IconBtnState();
}

class _IconBtnState extends State<_IconBtn> {
  double _scale = 1;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) => setState(() => _scale = 0.86),
      onTapCancel: () => setState(() => _scale = 1),
      onTapUp: (_) => setState(() => _scale = 1),
      onTap: () {
        HapticFeedback.selectionClick();
        widget.onTap();
      },
      child: AnimatedScale(
        duration: const Duration(milliseconds: 130),
        curve: Curves.easeOut,
        scale: _scale,
        child: Icon(widget.icon, color: Colors.white, size: widget.size),
      ),
    );
  }
}

class _PlayPause extends ConsumerStatefulWidget {
  final bool playing;
  const _PlayPause({required this.playing});

  @override
  ConsumerState<_PlayPause> createState() => _PlayPauseState();
}

class _PlayPauseState extends ConsumerState<_PlayPause> {
  double _scale = 1;

  @override
  Widget build(BuildContext context) {
    final buffering = ref.watch(isBufferingProvider).value ?? false;
    return GestureDetector(
      onTapDown: (_) => setState(() => _scale = 0.88),
      onTapCancel: () => setState(() => _scale = 1),
      onTapUp: (_) => setState(() => _scale = 1),
      onTap: () {
        HapticFeedback.mediumImpact();
        ref.read(playerControllerProvider).togglePlay();
      },
      child: AnimatedScale(
        duration: const Duration(milliseconds: 140),
        curve: Curves.easeOut,
        scale: _scale,
        child: buffering
            ? const SizedBox(
                width: 76,
                height: 76,
                child: CircularProgressIndicator(
                  strokeWidth: 3,
                  valueColor: AlwaysStoppedAnimation(Colors.white),
                ),
              )
            : AnimatedSwitcher(
                duration: const Duration(milliseconds: 220),
                transitionBuilder: (child, anim) =>
                    ScaleTransition(scale: anim, child: child),
                child: Icon(
                  widget.playing
                      ? Icons.pause_rounded
                      : Icons.play_arrow_rounded,
                  key: ValueKey(widget.playing),
                  color: Colors.white,
                  size: 76,
                ),
              ),
      ),
    );
  }
}

class _BottomRow extends StatelessWidget {
  final Song song;
  const _BottomRow({required this.song});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        TextButton.icon(
          onPressed: () => Navigator.of(context).push(
            MaterialPageRoute<void>(
              builder: (_) => LyricsScreen(song: song),
            ),
          ),
          icon: const Icon(Icons.chat_bubble_outline,
              color: Colors.white, size: 22),
          label: const Text(
            'Lyrics',
            style: TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w600,
              fontSize: 14,
            ),
          ),
        ),
        TextButton.icon(
          onPressed: () => Navigator.of(context).push(
            MaterialPageRoute<void>(builder: (_) => const QueueScreen()),
          ),
          icon: const Icon(Icons.queue_music_rounded,
              color: Colors.white, size: 22),
          label: const Text(
            'Queue',
            style: TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w600,
              fontSize: 14,
            ),
          ),
        ),
      ],
    );
  }
}

void _showOverflow(BuildContext context, WidgetRef ref, Song song) {
  final speed = SettingsService.instance.playbackSpeed;
  final crossfade = SettingsService.instance.crossfadeSeconds;
  final handler = ref.read(audioHandlerProvider);
  final shuffleOn = handler.shuffleEnabled;
  final loop = handler.loopMode;

  showModalBottomSheet<void>(
    context: context,
    backgroundColor: const Color(0xFF1B1B1B),
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
    ),
    builder: (sheetCtx) {
      return SafeArea(
        top: false,
        child: ListView(
          shrinkWrap: true,
          padding: const EdgeInsets.symmetric(vertical: 12),
          children: [
            _OverflowTile(
              icon: shuffleOn ? Icons.shuffle_on : Icons.shuffle,
              title: shuffleOn ? 'Shuffle: On' : 'Shuffle: Off',
              onTap: () {
                ref.read(playerControllerProvider).setShuffle(!shuffleOn);
                Navigator.pop(sheetCtx);
              },
            ),
            _OverflowTile(
              icon: _repeatIcon(loop),
              title: 'Repeat: ${_repeatLabel(loop)}',
              onTap: () {
                final next = _nextLoop(loop);
                ref.read(playerControllerProvider).setRepeat(next);
                Navigator.pop(sheetCtx);
              },
            ),
            _OverflowTile(
              icon: Icons.speed,
              title: 'Speed (${speed.toStringAsFixed(2)}x)',
              onTap: () {
                Navigator.pop(sheetCtx);
                _showSpeedSheet(context, ref);
              },
            ),
            _OverflowTile(
              icon: Icons.bedtime_outlined,
              title: 'Sleep Timer',
              onTap: () {
                Navigator.pop(sheetCtx);
                Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => const SleepTimerScreen(),
                ));
              },
            ),
            _OverflowTile(
              icon: Icons.swap_horiz,
              title: 'Crossfade (${crossfade}s)',
              onTap: () {
                Navigator.pop(sheetCtx);
                _showCrossfadeSheet(context, ref);
              },
            ),
            _OverflowTile(
              icon: Icons.graphic_eq,
              title: 'Equalizer',
              onTap: () {
                Navigator.pop(sheetCtx);
                Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => const EqualizerScreen(),
                ));
              },
            ),
            _OverflowTile(
              icon: Icons.share_outlined,
              title: 'Share',
              onTap: () {
                Navigator.pop(sheetCtx);
                Share.share(
                  'Check out "${song.title}" by ${song.artist} '
                  'https://youtu.be/${song.videoId}',
                );
              },
            ),
          ],
        ),
      );
    },
  );
}

class _OverflowTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final VoidCallback onTap;

  const _OverflowTile({
    required this.icon,
    required this.title,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: Colors.white),
      title: Text(title, style: const TextStyle(color: Colors.white)),
      onTap: onTap,
    );
  }
}

IconData _repeatIcon(LoopMode mode) {
  switch (mode) {
    case LoopMode.one:
      return Icons.repeat_one;
    case LoopMode.all:
      return Icons.repeat_on;
    case LoopMode.off:
      return Icons.repeat;
  }
}

String _repeatLabel(LoopMode mode) {
  switch (mode) {
    case LoopMode.one:
      return 'One';
    case LoopMode.all:
      return 'All';
    case LoopMode.off:
      return 'Off';
  }
}

LoopMode _nextLoop(LoopMode mode) {
  switch (mode) {
    case LoopMode.off:
      return LoopMode.all;
    case LoopMode.all:
      return LoopMode.one;
    case LoopMode.one:
      return LoopMode.off;
  }
}

void _showSpeedSheet(BuildContext context, WidgetRef ref) {
  showModalBottomSheet<void>(
    context: context,
    backgroundColor: const Color(0xFF1B1B1B),
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
    ),
    builder: (_) {
      return StatefulBuilder(builder: (ctx, setSheet) {
        final speed = SettingsService.instance.playbackSpeed;
        return Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                'Playback speed',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w700,
                  fontSize: 16,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '${speed.toStringAsFixed(2)}x',
                style: const TextStyle(
                  color: AppColors.accent,
                  fontWeight: FontWeight.w800,
                  fontSize: 28,
                ),
              ),
              Slider(
                value: speed,
                min: 0.5,
                max: 2.0,
                divisions: 30,
                activeColor: AppColors.accent,
                onChanged: (v) async {
                  await ref.read(playerControllerProvider).setSpeed(v);
                  setSheet(() {});
                },
              ),
              Wrap(
                spacing: 8,
                children: [
                  for (final v in const [0.5, 0.75, 1.0, 1.25, 1.5, 2.0])
                    OutlinedButton(
                      onPressed: () async {
                        await ref.read(playerControllerProvider).setSpeed(v);
                        setSheet(() {});
                      },
                      child: Text('${v}x',
                          style: const TextStyle(color: Colors.white)),
                    ),
                ],
              ),
              const SizedBox(height: 12),
            ],
          ),
        );
      });
    },
  );
}

void _showCrossfadeSheet(BuildContext context, WidgetRef ref) {
  showModalBottomSheet<void>(
    context: context,
    backgroundColor: const Color(0xFF1B1B1B),
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
    ),
    builder: (_) {
      return StatefulBuilder(builder: (ctx, setSheet) {
        final cf = SettingsService.instance.crossfadeSeconds;
        return Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                'Crossfade',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w700,
                  fontSize: 16,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '${cf}s',
                style: const TextStyle(
                  color: AppColors.accent,
                  fontWeight: FontWeight.w800,
                  fontSize: 28,
                ),
              ),
              Slider(
                value: cf.toDouble(),
                min: 0,
                max: 12,
                divisions: 12,
                activeColor: AppColors.accent,
                onChanged: (v) async {
                  await ref
                      .read(playerControllerProvider)
                      .setCrossfade(Duration(seconds: v.toInt()));
                  setSheet(() {});
                },
              ),
              const SizedBox(height: 12),
            ],
          ),
        );
      });
    },
  );
}
