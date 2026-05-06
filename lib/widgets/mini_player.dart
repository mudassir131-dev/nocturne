import 'dart:ui';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../screens/player_screen.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';

/// Floating mini player rendered above the dock when a song is loaded.
class MiniPlayer extends ConsumerWidget {
  const MiniPlayer({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final songAsync = ref.watch(currentSongProvider);
    final song = songAsync.value;
    if (song == null) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 25, sigmaY: 25),
          child: Container(
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.10),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: Colors.white.withOpacity(0.20),
                width: 1.2,
              ),
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
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.w600,
                                  fontSize: 13,
                                ),
                              ),
                              Text(
                                song.artist,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  color: Colors.white70,
                                  fontSize: 12,
                                ),
                              ),
                            ],
                          ),
                        ),
                        _PlayPauseButton(),
                        IconButton(
                          icon: const Icon(Icons.skip_next,
                              color: Colors.white, size: 26),
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
    );
  }

  void _openPlayer(BuildContext context) {
    Navigator.of(context).push(PageRouteBuilder(
      transitionDuration: const Duration(milliseconds: 350),
      reverseTransitionDuration: const Duration(milliseconds: 280),
      pageBuilder: (_, __, ___) => const PlayerScreen(),
      transitionsBuilder: (_, animation, __, child) {
        final tween =
            Tween(begin: const Offset(0, 1), end: Offset.zero).animate(
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
    if (song.thumbnail.isEmpty) {
      return Container(
        width: 40,
        height: 40,
        color: AppColors.card,
        child: const Icon(Icons.music_note,
            color: Colors.white54, size: 20),
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
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final playing = ref.watch(isPlayingProvider).value ?? false;
    return IconButton(
      icon: Icon(
        playing ? Icons.pause : Icons.play_arrow,
        color: Colors.white,
        size: 28,
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
    return SizedBox(
      height: 2,
      child: LinearProgressIndicator(
        value: total > 0 ? progress : null,
        backgroundColor: Colors.white.withOpacity(0.15),
        valueColor: const AlwaysStoppedAnimation(AppColors.accent),
      ),
    );
  }
}
