import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';
import 'package:palette_generator/palette_generator.dart';

import '../models/song.dart';
import '../services/audio_service.dart';
import '../services/database_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import 'equalizer_screen.dart';

/// Full-screen "now playing" view. Extracts the dominant colour of the
/// current album art via palette_generator and uses it as a soft gradient
/// background.
class PlayerScreen extends ConsumerStatefulWidget {
  const PlayerScreen({super.key});

  @override
  ConsumerState<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends ConsumerState<PlayerScreen> {
  Color _bgColor = AppColors.card;
  String? _lastArt;

  Future<void> _refreshPalette(String? artUrl) async {
    if (artUrl == null || artUrl.isEmpty || artUrl == _lastArt) return;
    _lastArt = artUrl;
    try {
      final palette = await PaletteGenerator.fromImageProvider(
        CachedNetworkImageProvider(artUrl),
        size: const Size(200, 200),
      );
      final c =
          palette.dominantColor?.color ?? palette.vibrantColor?.color;
      if (mounted && c != null) {
        setState(() => _bgColor = c);
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
        body: AnimatedContainer(
          duration: const Duration(milliseconds: 600),
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [
                _bgColor.withOpacity(0.65),
                _bgColor.withOpacity(0.15),
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
                  const SizedBox(height: 16),
                  _ProgressSection(),
                  const SizedBox(height: 8),
                  _Controls(),
                  const SizedBox(height: 16),
                  const _VolumeSection(),
                  const SizedBox(height: 16),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _TopBar extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
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

class _AlbumArt extends StatelessWidget {
  final Song song;
  const _AlbumArt({required this.song});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Hero(
        tag: 'player-art-${song.videoId}',
        child: ClipRRect(
          borderRadius: BorderRadius.circular(AppRadius.player),
          child: AspectRatio(
            aspectRatio: 1,
            child: song.thumbnail.isEmpty
                ? Container(
                    color: AppColors.card,
                    child: const Icon(Icons.music_note,
                        size: 96, color: Colors.white54),
                  )
                : CachedNetworkImage(
                    imageUrl: song.thumbnail,
                    fit: BoxFit.cover,
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
            if (liked) {
              await db.unlikeSong(song.videoId);
            } else {
              await db.likeSong(song);
            }
            // Force rebuild via provider invalidation.
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
    final value = total > 0
        ? pos.inMilliseconds.toDouble().clamp(0.0, total)
        : 0.0;

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
        IconButton(
          icon: Icon(
            Icons.shuffle,
            color: _shuffle ? AppColors.accent : Colors.white,
            size: 26,
          ),
          onPressed: () {
            setState(() => _shuffle = !_shuffle);
            ctrl.setShuffle(_shuffle);
          },
        ),
        IconButton(
          icon: const Icon(Icons.skip_previous,
              color: Colors.white, size: 36),
          onPressed: ctrl.previous,
        ),
        Container(
          width: 72,
          height: 72,
          decoration: const BoxDecoration(
            color: AppColors.accent,
            shape: BoxShape.circle,
          ),
          child: IconButton(
            icon: Icon(
              playing ? Icons.pause : Icons.play_arrow,
              color: Colors.white,
              size: 38,
            ),
            onPressed: ctrl.togglePlay,
          ),
        ),
        IconButton(
          icon: const Icon(Icons.skip_next, color: Colors.white, size: 36),
          onPressed: ctrl.next,
        ),
        IconButton(
          icon: Icon(
            _repeat == LoopMode.one ? Icons.repeat_one : Icons.repeat,
            color: _repeat == LoopMode.off ? Colors.white : AppColors.accent,
            size: 26,
          ),
          onPressed: () {
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

class _VolumeSection extends ConsumerStatefulWidget {
  const _VolumeSection();

  @override
  ConsumerState<_VolumeSection> createState() => _VolumeSectionState();
}

class _VolumeSectionState extends ConsumerState<_VolumeSection> {
  double _volume = 1.0;

  @override
  Widget build(BuildContext context) {
    final handler = ref.read(playerControllerProvider);
    return Row(
      children: [
        const Icon(Icons.volume_down, color: Colors.white70),
        Expanded(
          child: Slider(
            value: _volume,
            onChanged: (v) {
              setState(() => _volume = v);
              ref.read(audioHandlerProvider).player.setVolume(v);
              // Reference handler to keep linter happy when extending.
              handler.toString();
            },
          ),
        ),
        const Icon(Icons.volume_up, color: Colors.white70),
      ],
    );
  }
}
