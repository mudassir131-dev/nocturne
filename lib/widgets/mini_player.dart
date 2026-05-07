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
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(20),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 25, sigmaY: 25),
            child: Container(
              decoration: BoxDecoration(
                color: glassFill,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: glassBorder, width: 1.2),
              ),
              child: GestureDetector(
                onTap: () => _openPlayer(context),
                behavior: HitTestBehavior.opaque,
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
      onPressed: () => ref.read(playerControllerProvider).togglePlay(),
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
