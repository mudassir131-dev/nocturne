import 'dart:ui';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:palette_generator/palette_generator.dart';

import '../models/song.dart';
import '../screens/player_screen.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';

/// Floating mini player rendered above the dock when a song is loaded.
///
/// Behaviour:
/// - **Tap** anywhere on the bar opens the full player.
/// - **Swipe up** opens the full player (hero-animated).
/// - **Swipe right** plays the next song.
/// - **Swipe left** plays the previous song.
///
/// Background uses the album-art dominant colour (extracted via
/// [PaletteGenerator]) blended with a frosted glass surface.
class MiniPlayer extends ConsumerStatefulWidget {
  const MiniPlayer({super.key});

  @override
  ConsumerState<MiniPlayer> createState() => _MiniPlayerState();
}

class _MiniPlayerState extends ConsumerState<MiniPlayer> {
  Color? _dominant;
  String? _paletteFor;

  Future<void> _refreshPalette(String? thumbnail) async {
    if (thumbnail == null || thumbnail.isEmpty) return;
    if (_paletteFor == thumbnail) return;
    _paletteFor = thumbnail;
    try {
      final p = await PaletteGenerator.fromImageProvider(
        CachedNetworkImageProvider(thumbnail),
        size: const Size(80, 80),
        maximumColorCount: 6,
      );
      if (!mounted) return;
      setState(() => _dominant = p.dominantColor?.color);
    } catch (_) {
      // Palette generation is best-effort; ignore failures.
    }
  }

  @override
  Widget build(BuildContext context) {
    final songAsync = ref.watch(currentSongProvider);
    final song = songAsync.value;
    if (song == null) return const SizedBox.shrink();

    _refreshPalette(song.thumbnail);

    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final fg = theme.colorScheme.onSurface;
    final tint = _dominant ?? (isDark ? Colors.white : Colors.black);
    final glassFill = Color.alphaBlend(
      tint.withOpacity(isDark ? 0.18 : 0.10),
      isDark
          ? Colors.white.withOpacity(0.04)
          : Colors.black.withOpacity(0.02),
    );
    final glassBorder = isDark
        ? Colors.white.withOpacity(0.20)
        : Colors.black.withOpacity(0.08);

    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 280),
      transitionBuilder: (child, anim) => FadeTransition(
        opacity: anim,
        child: SlideTransition(
          position: Tween(
            begin: const Offset(0, 0.3),
            end: Offset.zero,
          ).animate(anim),
          child: child,
        ),
      ),
      child: Padding(
        key: ValueKey(song.videoId),
        padding: const EdgeInsets.fromLTRB(12, 0, 12, 4),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(18),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 30, sigmaY: 30),
            child: Container(
              decoration: BoxDecoration(
                color: glassFill,
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: glassBorder, width: 1.0),
              ),
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () => _openPlayer(context),
                onVerticalDragEnd: (d) {
                  final v = d.primaryVelocity ?? 0;
                  if (v < -250) _openPlayer(context);
                },
                onHorizontalDragEnd: (d) {
                  final v = d.primaryVelocity ?? 0;
                  if (v < -250) {
                    HapticFeedback.selectionClick();
                    ref.read(playerControllerProvider).next();
                  } else if (v > 250) {
                    HapticFeedback.selectionClick();
                    ref.read(playerControllerProvider).previous();
                  }
                },
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const _ProgressBar(),
                    Padding(
                      padding: const EdgeInsets.fromLTRB(8, 6, 8, 8),
                      child: Row(
                        children: [
                          Hero(
                            tag: 'player-art-${song.videoId}',
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: _Art(song: song),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                _ScrollingText(
                                  text: song.title,
                                  style: TextStyle(
                                    color: fg,
                                    fontWeight: FontWeight.w600,
                                    fontSize: 13,
                                  ),
                                ),
                                Text(
                                  song.artist,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(
                                    color: fg.withOpacity(0.7),
                                    fontSize: 12,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          _PlayPauseButton(color: fg),
                          IconButton(
                            icon: Icon(Icons.skip_next, color: fg, size: 26),
                            onPressed: () {
                              HapticFeedback.selectionClick();
                              ref.read(playerControllerProvider).next();
                            },
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  void _openPlayer(BuildContext context) {
    Navigator.of(context).push(PageRouteBuilder(
      transitionDuration: const Duration(milliseconds: 380),
      reverseTransitionDuration: const Duration(milliseconds: 280),
      pageBuilder: (_, __, ___) => const PlayerScreen(),
      transitionsBuilder: (_, animation, __, child) {
        final tween = Tween(
          begin: const Offset(0, 1),
          end: Offset.zero,
        ).animate(
          CurvedAnimation(parent: animation, curve: Curves.easeOutCubic),
        );
        return SlideTransition(position: tween, child: child);
      },
    ));
  }
}

class _Art extends StatelessWidget {
  final Song song;
  const _Art({required this.song});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    if (song.thumbnail.isEmpty) {
      return Container(
        width: 40,
        height: 40,
        color: theme.cardColor,
        child: Icon(
          Icons.music_note,
          color: theme.colorScheme.onSurface.withOpacity(0.5),
          size: 20,
        ),
      );
    }
    return CachedNetworkImage(
      imageUrl: song.thumbnail,
      width: 40,
      height: 40,
      fit: BoxFit.cover,
    );
  }
}

class _PlayPauseButton extends ConsumerWidget {
  final Color color;
  const _PlayPauseButton({required this.color});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final playing = ref.watch(isPlayingProvider).value ?? false;
    return IconButton(
      icon: AnimatedSwitcher(
        duration: const Duration(milliseconds: 220),
        transitionBuilder: (child, anim) =>
            ScaleTransition(scale: anim, child: child),
        child: Icon(
          playing ? Icons.pause : Icons.play_arrow,
          key: ValueKey(playing),
          color: color,
          size: 28,
        ),
      ),
      onPressed: () {
        HapticFeedback.selectionClick();
        ref.read(playerControllerProvider).togglePlay();
      },
    );
  }
}

class _ProgressBar extends ConsumerWidget {
  const _ProgressBar();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final pos = ref.watch(positionProvider).value ?? Duration.zero;
    final dur = ref.watch(durationProvider).value;
    final total = dur?.inMilliseconds ?? 0;
    final progress =
        total > 0 ? (pos.inMilliseconds / total).clamp(0.0, 1.0) : 0.0;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return SizedBox(
      height: 2,
      child: LinearProgressIndicator(
        value: total > 0 ? progress : null,
        backgroundColor:
            (isDark ? Colors.white : Colors.black).withOpacity(0.10),
        valueColor: const AlwaysStoppedAnimation(AppColors.accent),
      ),
    );
  }
}

/// Title row that auto-scrolls left when the title is too long to fit,
/// mirroring iOS's "running title" behaviour in the mini player.
class _ScrollingText extends StatefulWidget {
  final String text;
  final TextStyle style;
  const _ScrollingText({required this.text, required this.style});

  @override
  State<_ScrollingText> createState() => _ScrollingTextState();
}

class _ScrollingTextState extends State<_ScrollingText> {
  final ScrollController _ctrl = ScrollController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _scroll());
  }

  Future<void> _scroll() async {
    while (mounted && _ctrl.hasClients) {
      await Future<void>.delayed(const Duration(seconds: 2));
      if (!mounted || !_ctrl.hasClients) return;
      final m = _ctrl.position.maxScrollExtent;
      if (m <= 0) {
        await Future<void>.delayed(const Duration(seconds: 2));
        continue;
      }
      try {
        await _ctrl.animateTo(
          m,
          duration:
              Duration(milliseconds: (m * 25).clamp(2000, 8000).toInt()),
          curve: Curves.linear,
        );
      } catch (_) {
        return;
      }
      if (!mounted || !_ctrl.hasClients) return;
      await Future<void>.delayed(const Duration(seconds: 1));
      if (!mounted || !_ctrl.hasClients) return;
      _ctrl.jumpTo(0);
    }
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height:
          widget.style.fontSize == null ? 18 : widget.style.fontSize! * 1.4,
      child: SingleChildScrollView(
        controller: _ctrl,
        scrollDirection: Axis.horizontal,
        physics: const NeverScrollableScrollPhysics(),
        child: Text(
          widget.text,
          maxLines: 1,
          softWrap: false,
          style: widget.style,
        ),
      ),
    );
  }
}
