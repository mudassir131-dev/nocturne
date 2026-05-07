import 'dart:ui';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../screens/player_screen.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';

/// Floating mini player rendered above the dock when a song is loaded.
/// Theme-aware: uses translucent glass appropriate for the current
/// brightness.
class MiniPlayer extends ConsumerWidget {
  const MiniPlayer({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final songAsync = ref.watch(currentSongProvider);
    final song = songAsync.value;
    if (song == null) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final fg = theme.colorScheme.onSurface;
    final glassFill = isDark
        ? Colors.white.withOpacity(0.12)
        : Colors.black.withOpacity(0.04);
    final glassBorder = isDark
        ? Colors.white.withOpacity(0.25)
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
        padding: const EdgeInsets.fromLTRB(12, 0, 12, 8),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(28),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 24, sigmaY: 24),
            child: Container(
              decoration: BoxDecoration(
                color: glassFill,
                borderRadius: BorderRadius.circular(28),
                border: Border.all(color: glassBorder, width: 1),
              ),
              child: _SwipeableMiniBody(
                onTap: () => _openPlayer(context),
                onSwipeUp: () => _openPlayer(context),
                onSwipeRightNext: () =>
                    ref.read(playerControllerProvider).next(),
                onSwipeLeftPrev: () =>
                    ref.read(playerControllerProvider).previous(),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    _ProgressBar(),
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
                                Text(
                                  song.title,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
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
                            onPressed: () =>
                                ref.read(playerControllerProvider).next(),
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
      memCacheWidth: 160,
      memCacheHeight: 160,
    );
  }
}

class _PlayPauseButton extends ConsumerWidget {
  final Color color;
  const _PlayPauseButton({required this.color});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final playing = ref.watch(isPlayingProvider).value ?? false;
    final buffering = ref.watch(isBufferingProvider).value ?? false;
    return IconButton(
      icon: buffering
          ? SizedBox(
              width: 22,
              height: 22,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                valueColor: AlwaysStoppedAnimation(color),
              ),
            )
          : AnimatedSwitcher(
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
      onPressed: () => ref.read(playerControllerProvider).togglePlay(),
    );
  }
}

/// Wraps the mini-player body with horizontal + vertical drag detection.
/// Drag up → open full player. Drag right → next song. Drag left → previous.
class _SwipeableMiniBody extends StatefulWidget {
  final Widget child;
  final VoidCallback onTap;
  final VoidCallback onSwipeUp;
  final VoidCallback onSwipeRightNext;
  final VoidCallback onSwipeLeftPrev;

  const _SwipeableMiniBody({
    required this.child,
    required this.onTap,
    required this.onSwipeUp,
    required this.onSwipeRightNext,
    required this.onSwipeLeftPrev,
  });

  @override
  State<_SwipeableMiniBody> createState() => _SwipeableMiniBodyState();
}

class _SwipeableMiniBodyState extends State<_SwipeableMiniBody> {
  double _dx = 0;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: widget.onTap,
      onVerticalDragEnd: (d) {
        final v = d.primaryVelocity ?? 0;
        if (v < -300) widget.onSwipeUp();
      },
      onHorizontalDragUpdate: (d) {
        setState(() => _dx += d.delta.dx);
      },
      onHorizontalDragEnd: (d) {
        final v = d.primaryVelocity ?? 0;
        if (v > 600 || _dx > 80) {
          widget.onSwipeRightNext();
        } else if (v < -600 || _dx < -80) {
          widget.onSwipeLeftPrev();
        }
        setState(() => _dx = 0);
      },
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 120),
        transform: Matrix4.translationValues(_dx * 0.4, 0, 0),
        child: widget.child,
      ),
    );
  }
}

class _ProgressBar extends ConsumerWidget {
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
        backgroundColor: (isDark ? Colors.white : Colors.black).withOpacity(0.15),
        valueColor: const AlwaysStoppedAnimation(AppColors.accent),
      ),
    );
  }
}
