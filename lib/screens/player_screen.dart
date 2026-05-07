import 'dart:ui';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';
import 'package:palette_generator/palette_generator.dart';
import 'package:share_plus/share_plus.dart';

import '../models/song.dart';
import '../services/database_service.dart';
import '../services/settings_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import 'equalizer_screen.dart';
import 'lyrics_screen.dart';
import 'queue_screen.dart';
import 'sleep_timer_screen.dart';

/// Full-screen "now playing" view. Extracts the dominant colour of the
/// current album art via palette_generator and animates a soft gradient
/// background across track changes.
class PlayerScreen extends ConsumerStatefulWidget {
  const PlayerScreen({super.key});

  @override
  ConsumerState<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends ConsumerState<PlayerScreen> {
  Color _bgColor = AppColors.card;
  Color _prevBgColor = AppColors.card;
  String? _lastArt;

  Future<void> _refreshPalette(String? artUrl) async {
    if (artUrl == null || artUrl.isEmpty || artUrl == _lastArt) return;
    _lastArt = artUrl;
    try {
      final palette = await PaletteGenerator.fromImageProvider(
        CachedNetworkImageProvider(artUrl),
        size: const Size(200, 200),
      );
      final c = palette.dominantColor?.color ?? palette.vibrantColor?.color;
      if (mounted && c != null) {
        setState(() {
          _prevBgColor = _bgColor;
          _bgColor = c;
        });
      }
    } catch (_) {
      // Ignore palette failures and keep the default background.
    }
  }

  @override
  Widget build(BuildContext context) {
    final song = ref.watch(currentSongProvider).value;
    if (song == null) {
      return const Scaffold(
        backgroundColor: AppColors.background,
        body: Center(
          child: Text(
            'Nothing playing',
            style: TextStyle(color: AppColors.textSecondary),
          ),
        ),
      );
    }

    _refreshPalette(song.thumbnail);

    return GestureDetector(
      onVerticalDragEnd: (d) {
        if ((d.primaryVelocity ?? 0) > 200) {
          Navigator.of(context).maybePop();
        }
      },
      child: Scaffold(
        backgroundColor: AppColors.background,
        body: TweenAnimationBuilder<Color?>(
          tween: ColorTween(begin: _prevBgColor, end: _bgColor),
          duration: const Duration(milliseconds: 800),
          curve: Curves.easeInOut,
          builder: (context, value, _) {
            final c = value ?? _bgColor;
            return Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    c.withOpacity(0.65),
                    c.withOpacity(0.15),
                    AppColors.background,
                  ],
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                ),
              ),
              child: SafeArea(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Column(
                    children: [
                      _TopBar(),
                      const SizedBox(height: 16),
                      Expanded(child: _AlbumArt(song: song)),
                      const SizedBox(height: 24),
                      _TitleRow(song: song),
                      const SizedBox(height: 12),
                      _ProgressSection(),
                      const SizedBox(height: 4),
                      _Controls(),
                      const SizedBox(height: 12),
                      _ExtraControls(song: song),
                      const SizedBox(height: 8),
                    ],
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

class _TopBar extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final song = ref.watch(currentSongProvider).value;
    return Row(
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
            fontWeight: FontWeight.w600,
          ),
        ),
        const Spacer(),
        IconButton(
          tooltip: 'Lyrics',
          icon: const Icon(Icons.lyrics_outlined, color: Colors.white),
          onPressed: song == null
              ? null
              : () => Navigator.of(context).push(
                    MaterialPageRoute<void>(
                      builder: (_) => LyricsScreen(song: song),
                    ),
                  ),
        ),
        IconButton(
          tooltip: 'Equalizer',
          icon: const Icon(Icons.graphic_eq, color: Colors.white),
          onPressed: () => Navigator.of(context).push(
            MaterialPageRoute<void>(
              builder: (_) => const EqualizerScreen(),
            ),
          ),
        ),
      ],
    );
  }
}

class _AlbumArt extends ConsumerStatefulWidget {
  final Song song;
  const _AlbumArt({required this.song});

  @override
  ConsumerState<_AlbumArt> createState() => _AlbumArtState();
}

class _AlbumArtState extends ConsumerState<_AlbumArt>
    with SingleTickerProviderStateMixin {
  late final AnimationController _spin;

  @override
  void initState() {
    super.initState();
    _spin = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 18),
    );
  }

  @override
  void dispose() {
    _spin.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final playing = ref.watch(isPlayingProvider).value ?? false;
    if (playing) {
      _spin.repeat();
    } else {
      _spin.stop();
    }
    return Center(
      child: Hero(
        tag: 'player-art-${widget.song.videoId}',
        child: ClipRRect(
          borderRadius: BorderRadius.circular(AppRadius.player),
          child: AspectRatio(
            aspectRatio: 1,
            child: RotationTransition(
              turns: _spin,
              child: widget.song.thumbnail.isEmpty
                  ? Container(
                      color: AppColors.card,
                      child: const Icon(Icons.music_note,
                          size: 96, color: Colors.white54),
                    )
                  : CachedNetworkImage(
                      imageUrl: widget.song.thumbnail,
                      fit: BoxFit.cover,
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
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w800,
                  fontSize: 22,
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 4),
              Text(
                song.artist,
                style: const TextStyle(color: Colors.white70),
              ),
            ],
          ),
        ),
        IconButton(
          icon: Icon(
            liked ? Icons.favorite : Icons.favorite_border,
            color: liked ? AppColors.accent : Colors.white,
            size: 28,
          ),
          onPressed: () async {
            HapticFeedback.lightImpact();
            if (liked) {
              await db.unlikeSong(song.videoId);
            } else {
              await db.likeSong(song);
            }
            ref.invalidate(databaseServiceProvider);
          },
        ),
      ],
    );
  }
}

class _ProgressSection extends ConsumerWidget {
  String _fmt(Duration d) {
    final m = d.inMinutes;
    final s = (d.inSeconds % 60).toString().padLeft(2, '0');
    return '$m:$s';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final pos = ref.watch(positionProvider).value ?? Duration.zero;
    final dur = ref.watch(durationProvider).value ?? Duration.zero;
    final total = dur.inMilliseconds.toDouble();
    final value =
        total > 0 ? pos.inMilliseconds.toDouble().clamp(0.0, total) : 0.0;

    return Column(
      children: [
        Slider(
          value: value,
          max: total > 0 ? total : 1,
          onChanged: total > 0
              ? (v) => ref
                  .read(playerControllerProvider)
                  .seek(Duration(milliseconds: v.toInt()))
              : null,
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                _fmt(pos),
                style: const TextStyle(color: Colors.white70),
              ),
              Text(
                total > 0 ? '-${_fmt(dur - pos)}' : '--:--',
                style: const TextStyle(color: Colors.white70),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _Controls extends ConsumerStatefulWidget {
  @override
  ConsumerState<_Controls> createState() => _ControlsState();
}

class _ControlsState extends ConsumerState<_Controls> {
  bool _shuffle = false;
  LoopMode _repeat = LoopMode.off;

  @override
  Widget build(BuildContext context) {
    final playing = ref.watch(isPlayingProvider).value ?? false;
    final ctrl = ref.read(playerControllerProvider);
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        _SpringIcon(
          icon: Icons.shuffle,
          color: _shuffle ? AppColors.accent : Colors.white,
          onTap: () {
            HapticFeedback.selectionClick();
            setState(() => _shuffle = !_shuffle);
            ctrl.setShuffle(_shuffle);
          },
        ),
        _SpringIcon(
          icon: Icons.skip_previous,
          size: 38,
          color: Colors.white,
          onTap: () {
            HapticFeedback.selectionClick();
            ctrl.previous();
          },
        ),
        _PlayPauseButton(
          playing: playing,
          onTap: () {
            HapticFeedback.mediumImpact();
            ctrl.togglePlay();
          },
        ),
        _SpringIcon(
          icon: Icons.skip_next,
          size: 38,
          color: Colors.white,
          onTap: () {
            HapticFeedback.selectionClick();
            ctrl.next();
          },
        ),
        _SpringIcon(
          icon: _repeat == LoopMode.one ? Icons.repeat_one : Icons.repeat,
          color: _repeat == LoopMode.off ? Colors.white : AppColors.accent,
          onTap: () {
            HapticFeedback.selectionClick();
            final next = switch (_repeat) {
              LoopMode.off => LoopMode.all,
              LoopMode.all => LoopMode.one,
              LoopMode.one => LoopMode.off,
            };
            setState(() => _repeat = next);
            ctrl.setRepeat(next);
          },
        ),
      ],
    );
  }
}

/// Springy play/pause: scales down on press and back up on release with
/// an elastic curve.
class _PlayPauseButton extends StatefulWidget {
  final bool playing;
  final VoidCallback onTap;
  const _PlayPauseButton({required this.playing, required this.onTap});

  @override
  State<_PlayPauseButton> createState() => _PlayPauseButtonState();
}

class _PlayPauseButtonState extends State<_PlayPauseButton> {
  double _scale = 1.0;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) => setState(() => _scale = 0.92),
      onTapCancel: () => setState(() => _scale = 1.0),
      onTapUp: (_) {
        setState(() => _scale = 1.0);
        widget.onTap();
      },
      child: AnimatedScale(
        scale: _scale,
        duration: const Duration(milliseconds: 160),
        curve: Curves.easeOut,
        child: Container(
          width: 72,
          height: 72,
          decoration: BoxDecoration(
            color: AppColors.accent,
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: AppColors.accent.withOpacity(0.45),
                blurRadius: 24,
                spreadRadius: 1,
              ),
            ],
          ),
          child: AnimatedSwitcher(
            duration: const Duration(milliseconds: 240),
            switchInCurve: Curves.easeOut,
            transitionBuilder: (child, anim) =>
                ScaleTransition(scale: anim, child: child),
            child: Icon(
              widget.playing ? Icons.pause : Icons.play_arrow,
              key: ValueKey(widget.playing),
              color: Colors.white,
              size: 38,
            ),
          ),
        ),
      ),
    );
  }
}

class _SpringIcon extends StatefulWidget {
  final IconData icon;
  final Color color;
  final VoidCallback onTap;
  final double size;
  const _SpringIcon({
    required this.icon,
    required this.color,
    required this.onTap,
    this.size = 26,
  });

  @override
  State<_SpringIcon> createState() => _SpringIconState();
}

class _SpringIconState extends State<_SpringIcon> {
  double _scale = 1.0;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) => setState(() => _scale = 0.85),
      onTapCancel: () => setState(() => _scale = 1.0),
      onTapUp: (_) {
        setState(() => _scale = 1.0);
        widget.onTap();
      },
      child: AnimatedScale(
        scale: _scale,
        duration: const Duration(milliseconds: 140),
        curve: Curves.easeOut,
        child: Padding(
          padding: const EdgeInsets.all(10),
          child: Icon(widget.icon, color: widget.color, size: widget.size),
        ),
      ),
    );
  }
}

/// Bottom row exposing speed / queue / sleep / share controls.
class _ExtraControls extends ConsumerWidget {
  final Song song;
  const _ExtraControls({required this.song});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        _ExtraButton(
          icon: Icons.speed,
          label: '${ref.watch(_speedProvider).toStringAsFixed(2)}x',
          onTap: () => _showSpeedSheet(context, ref),
        ),
        _ExtraButton(
          icon: Icons.queue_music,
          label: 'Queue',
          onTap: () => Navigator.of(context).push(MaterialPageRoute<void>(
            builder: (_) => const QueueScreen(),
          )),
        ),
        _ExtraButton(
          icon: Icons.bedtime_outlined,
          label: 'Sleep',
          onTap: () => Navigator.of(context).push(MaterialPageRoute<void>(
            builder: (_) => const SleepTimerScreen(),
          )),
        ),
        _ExtraButton(
          icon: Icons.share_outlined,
          label: 'Share',
          onTap: () {
            final url = 'https://youtu.be/${song.videoId}';
            Share.share(
              '${song.title} — ${song.artist}\n$url',
              subject: song.title,
            );
          },
        ),
      ],
    );
  }
}

class _ExtraButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  const _ExtraButton({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return TextButton.icon(
      style: TextButton.styleFrom(foregroundColor: Colors.white),
      onPressed: () {
        HapticFeedback.selectionClick();
        onTap();
      },
      icon: Icon(icon, color: Colors.white70, size: 20),
      label: Text(
        label,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 13,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}

final _speedProvider = StateProvider<double>(
  (_) => SettingsService.instance.playbackSpeed,
);

void _showSpeedSheet(BuildContext context, WidgetRef ref) {
  final theme = Theme.of(context);
  final fg = theme.colorScheme.onSurface;
  showModalBottomSheet<void>(
    context: context,
    backgroundColor: Colors.transparent,
    builder: (ctx) => BackdropFilter(
      filter: ImageFilter.blur(sigmaX: 25, sigmaY: 25),
      child: Container(
        decoration: BoxDecoration(
          color: theme.cardColor.withOpacity(0.85),
          borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
        ),
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
        child: StatefulBuilder(
          builder: (_, setState) {
            final speed = ref.watch(_speedProvider);
            return Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  'Playback speed',
                  style: TextStyle(
                    color: fg,
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
                  onChanged: (v) {
                    setState(() {});
                    ref.read(_speedProvider.notifier).state = v;
                    ref.read(playerControllerProvider).setSpeed(v);
                  },
                ),
                const SizedBox(height: 8),
                Wrap(
                  alignment: WrapAlignment.center,
                  spacing: 8,
                  children: [
                    for (final s in const [0.5, 0.75, 1.0, 1.25, 1.5, 2.0])
                      ChoiceChip(
                        label: Text('${s}x'),
                        selected: (speed - s).abs() < 0.01,
                        onSelected: (_) {
                          setState(() {});
                          ref.read(_speedProvider.notifier).state = s;
                          ref.read(playerControllerProvider).setSpeed(s);
                        },
                      ),
                  ],
                ),
                const SizedBox(height: 24),
                Text(
                  'Crossfade',
                  style: TextStyle(
                    color: fg,
                    fontWeight: FontWeight.w700,
                    fontSize: 16,
                  ),
                ),
                _CrossfadeSlider(),
                const SizedBox(height: 8),
              ],
            );
          },
        ),
      ),
    ),
  );
}

class _CrossfadeSlider extends ConsumerStatefulWidget {
  @override
  ConsumerState<_CrossfadeSlider> createState() => _CrossfadeSliderState();
}

class _CrossfadeSliderState extends ConsumerState<_CrossfadeSlider> {
  late int _seconds;

  @override
  void initState() {
    super.initState();
    _seconds = SettingsService.instance.crossfadeSeconds;
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Slider(
          value: _seconds.toDouble(),
          min: 0,
          max: 12,
          divisions: 12,
          label: _seconds == 0 ? 'Off' : '${_seconds}s',
          onChanged: (v) {
            setState(() => _seconds = v.toInt());
            ref
                .read(playerControllerProvider)
                .setCrossfade(Duration(seconds: _seconds));
          },
        ),
        Text(
          _seconds == 0
              ? 'No crossfade'
              : 'Fade ${_seconds}s between songs',
          style: TextStyle(
            color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
          ),
        ),
      ],
    );
  }
}
