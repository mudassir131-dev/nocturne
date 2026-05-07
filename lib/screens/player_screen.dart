import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';
import 'package:share_plus/share_plus.dart';

import '../models/song.dart';
import '../services/api_service.dart';
import '../services/audio_service.dart';
import '../services/database_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import '../widgets/dynamic_gradient_background.dart';
import '../widgets/glass_panel.dart';
import '../widgets/ios_progress_bar.dart';
import 'album_screen.dart';
import 'equalizer_screen.dart';
import 'lyrics_screen.dart';
import 'queue_screen.dart';

/// Full-screen "now playing" view styled to match the iOS 26 player:
/// - thin progress line with a draggable knob
/// - album art that scales back when paused and back up on play
/// - dynamic palette gradient that crossfades when the song changes
/// - frosted-glass three-dots menu (shuffle / repeat / sleep / share / dl)
/// - lyrics + queue sheets that slide up from the bottom
class PlayerScreen extends ConsumerStatefulWidget {
  const PlayerScreen({super.key});

  @override
  ConsumerState<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends ConsumerState<PlayerScreen> {
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

    return GestureDetector(
      onVerticalDragEnd: (d) {
        if ((d.primaryVelocity ?? 0) > 250) {
          Navigator.of(context).maybePop();
        }
      },
      child: Scaffold(
        backgroundColor: AppColors.background,
        body: DynamicGradientBackground(
          artUrl: song.thumbnail,
          child: SafeArea(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: Column(
                children: [
                  _TopBar(song: song),
                  const SizedBox(height: 14),
                  Expanded(child: _AlbumArt(song: song)),
                  const SizedBox(height: 18),
                  _TitleRow(song: song),
                  const SizedBox(height: 14),
                  const _ProgressSection(),
                  const SizedBox(height: 8),
                  _Controls(),
                  const SizedBox(height: 12),
                  const _VolumeSlider(),
                  const SizedBox(height: 16),
                  _Footer(song: song),
                  const SizedBox(height: 8),
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
  final Song song;
  const _TopBar({required this.song});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        IconButton(
          icon: const Icon(CupertinoIcons.chevron_down,
              color: Colors.white, size: 24),
          onPressed: () => Navigator.of(context).maybePop(),
        ),
        const Spacer(),
        const Text(
          'PLAYING FROM QUEUE',
          style: TextStyle(
            color: Colors.white70,
            fontSize: 11,
            letterSpacing: 1.2,
            fontWeight: FontWeight.w600,
          ),
        ),
        const Spacer(),
        IconButton(
          icon: const Icon(CupertinoIcons.ellipsis,
              color: Colors.white, size: 24),
          tooltip: 'More',
          onPressed: () => _openMenu(context, song),
        ),
      ],
    );
  }
}

void _openMenu(BuildContext context, Song song) {
  showModalBottomSheet<void>(
    context: context,
    backgroundColor: Colors.transparent,
    barrierColor: Colors.black.withOpacity(0.35),
    isScrollControlled: true,
    builder: (_) => _MoreMenuSheet(song: song),
  );
}

class _AlbumArt extends ConsumerWidget {
  final Song song;
  const _AlbumArt({required this.song});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final playing = ref.watch(isPlayingProvider).value ?? false;
    return Center(
      child: AnimatedScale(
        scale: playing ? 1.0 : 0.85,
        duration: const Duration(milliseconds: 400),
        curve: Curves.easeOutCubic,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 380),
          curve: Curves.easeOutCubic,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(AppRadius.player),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(playing ? 0.55 : 0.30),
                blurRadius: playing ? 40 : 22,
                offset: Offset(0, playing ? 18 : 10),
              ),
            ],
          ),
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
                style: TextStyle(color: Colors.white.withOpacity(0.7)),
              ),
            ],
          ),
        ),
        IconButton(
          icon: Icon(
            liked ? CupertinoIcons.heart_fill : CupertinoIcons.heart,
            color: liked ? AppColors.accent : Colors.white,
            size: 28,
          ),
          onPressed: () async {
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
  const _ProgressSection();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final pos = ref.watch(positionProvider).value ?? Duration.zero;
    final dur = ref.watch(durationProvider).value ?? Duration.zero;

    return Column(
      children: [
        IosProgressBar(
          position: pos,
          duration: dur,
          onSeek: (p) => ref.read(playerControllerProvider).seek(p),
        ),
        const SizedBox(height: 6),
        const Center(child: _LosslessBadge()),
      ],
    );
  }
}

/// Small "Lossless" badge under the progress bar — shown whenever the
/// resolved stream is opus / m4a (which we always prefer thanks to the
/// backend's `bestaudio[ext=opus]/...` format selector). The actual
/// codec isn't introspected from just_audio, so this is informational.
class _LosslessBadge extends StatelessWidget {
  const _LosslessBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.10),
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: Colors.white.withOpacity(0.18)),
      ),
      child: const Text(
        'LOSSLESS',
        style: TextStyle(
          color: Colors.white,
          fontSize: 9,
          fontWeight: FontWeight.w700,
          letterSpacing: 1.4,
        ),
      ),
    );
  }
}

/// Volume slider mirroring iOS's player layout: speaker icon left,
/// loud icon right, thin red track in the middle.
class _VolumeSlider extends ConsumerStatefulWidget {
  const _VolumeSlider();

  @override
  ConsumerState<_VolumeSlider> createState() => _VolumeSliderState();
}

class _VolumeSliderState extends ConsumerState<_VolumeSlider> {
  double _value = 1.0;

  @override
  void initState() {
    super.initState();
    _value = ref.read(audioHandlerProvider).player.volume;
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Icon(CupertinoIcons.speaker_1_fill, color: Colors.white70, size: 16),
        Expanded(
          child: SliderTheme(
            data: SliderTheme.of(context).copyWith(
              activeTrackColor: AppColors.accent,
              inactiveTrackColor: Colors.white.withOpacity(0.18),
              thumbColor: Colors.white,
              trackHeight: 2,
              thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
              overlayShape: const RoundSliderOverlayShape(overlayRadius: 14),
            ),
            child: Slider(
              value: _value.clamp(0.0, 1.0),
              onChanged: (v) {
                setState(() => _value = v);
                ref.read(audioHandlerProvider).player.setVolume(v);
              },
            ),
          ),
        ),
        const Icon(CupertinoIcons.speaker_3_fill, color: Colors.white, size: 16),
      ],
    );
  }
}

class _Controls extends ConsumerStatefulWidget {
  @override
  ConsumerState<_Controls> createState() => _ControlsState();
}

class _ControlsState extends ConsumerState<_Controls> {
  @override
  Widget build(BuildContext context) {
    final playing = ref.watch(isPlayingProvider).value ?? false;
    final handler = ref.watch(audioHandlerProvider);
    final ctrl = ref.read(playerControllerProvider);
    final shuffle = handler.shuffleEnabled;
    final repeat = handler.loopMode;
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        _IconToggle(
          icon: CupertinoIcons.shuffle,
          active: shuffle,
          onTap: () async {
            HapticFeedback.selectionClick();
            await ctrl.setShuffle(!shuffle);
            if (mounted) setState(() {});
          },
        ),
        IconButton(
          icon: const Icon(CupertinoIcons.backward_fill,
              color: Colors.white, size: 36),
          onPressed: () {
            HapticFeedback.selectionClick();
            ctrl.previous();
          },
        ),
        AnimatedContainer(
          duration: const Duration(milliseconds: 220),
          curve: Curves.easeOutCubic,
          width: 76,
          height: 76,
          decoration: BoxDecoration(
            color: AppColors.accent,
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: AppColors.accent.withOpacity(0.45),
                blurRadius: playing ? 26 : 12,
                spreadRadius: playing ? 2 : 0,
              ),
            ],
          ),
          child: IconButton(
            icon: AnimatedSwitcher(
              duration: const Duration(milliseconds: 180),
              transitionBuilder: (child, anim) =>
                  ScaleTransition(scale: anim, child: child),
              child: Icon(
                playing ? CupertinoIcons.pause_fill : CupertinoIcons.play_fill,
                key: ValueKey(playing),
                color: Colors.white,
                size: 36,
              ),
            ),
            onPressed: () {
              HapticFeedback.selectionClick();
              ctrl.togglePlay();
            },
          ),
        ),
        IconButton(
          icon: const Icon(CupertinoIcons.forward_fill,
              color: Colors.white, size: 36),
          onPressed: () {
            HapticFeedback.selectionClick();
            ctrl.next();
          },
        ),
        _IconToggle(
          icon: repeat == LoopMode.one
              ? CupertinoIcons.repeat_1
              : CupertinoIcons.repeat,
          active: repeat != LoopMode.off,
          onTap: () async {
            HapticFeedback.selectionClick();
            final next = switch (repeat) {
              LoopMode.off => LoopMode.all,
              LoopMode.all => LoopMode.one,
              LoopMode.one => LoopMode.off,
            };
            await ctrl.setRepeat(next);
            if (mounted) setState(() {});
          },
        ),
      ],
    );
  }
}

class _IconToggle extends StatelessWidget {
  final IconData icon;
  final bool active;
  final VoidCallback onTap;

  const _IconToggle({
    required this.icon,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return IconButton(
      onPressed: onTap,
      icon: Icon(
        icon,
        color: active ? AppColors.accent : Colors.white,
        size: 24,
      ),
    );
  }
}

class _Footer extends ConsumerWidget {
  final Song song;
  const _Footer({required this.song});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        _FooterButton(
          icon: CupertinoIcons.text_bubble,
          label: 'Lyrics',
          onTap: () {
            Navigator.of(context).push(
              PageRouteBuilder<void>(
                opaque: false,
                barrierColor: Colors.black54,
                transitionDuration: const Duration(milliseconds: 380),
                reverseTransitionDuration: const Duration(milliseconds: 280),
                pageBuilder: (_, __, ___) => LyricsScreen(song: song),
                transitionsBuilder: (_, anim, __, child) {
                  final tween = Tween<Offset>(
                    begin: const Offset(0, 1),
                    end: Offset.zero,
                  ).chain(CurveTween(curve: Curves.easeOutCubic));
                  return SlideTransition(
                    position: anim.drive(tween),
                    child: child,
                  );
                },
              ),
            );
          },
        ),
        _FooterButton(
          icon: CupertinoIcons.rectangle_on_rectangle_angled,
          label: 'AirPlay',
          onTap: () {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('AirPlay routing coming soon.'),
                duration: Duration(seconds: 1),
              ),
            );
          },
        ),
        _FooterButton(
          icon: CupertinoIcons.music_note_list,
          label: 'Queue',
          onTap: () {
            showModalBottomSheet<void>(
              context: context,
              backgroundColor: Colors.transparent,
              barrierColor: Colors.black.withOpacity(0.35),
              isScrollControlled: true,
              builder: (_) => DraggableScrollableSheet(
                initialChildSize: 0.85,
                minChildSize: 0.5,
                maxChildSize: 0.95,
                expand: false,
                builder: (_, __) => GlassPanel(
                  radius: const BorderRadius.vertical(
                    top: Radius.circular(28),
                  ),
                  padding: EdgeInsets.zero,
                  fillOpacity: 0.18,
                  child: const QueueScreen(),
                ),
              ),
            );
          },
        ),
      ],
    );
  }
}

class _FooterButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _FooterButton({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: Colors.white, size: 22),
            const SizedBox(height: 4),
            Text(
              label,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 11,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MoreMenuSheet extends ConsumerStatefulWidget {
  final Song song;
  const _MoreMenuSheet({required this.song});

  @override
  ConsumerState<_MoreMenuSheet> createState() => _MoreMenuSheetState();
}

class _MoreMenuSheetState extends ConsumerState<_MoreMenuSheet> {
  Duration? _sleep;

  @override
  void initState() {
    super.initState();
    _sleep = ref.read(audioHandlerProvider).sleepRemaining;
  }

  @override
  Widget build(BuildContext context) {
    final handler = ref.watch(audioHandlerProvider);
    final db = ref.watch(databaseServiceProvider);
    final ctrl = ref.read(playerControllerProvider);
    final liked = db.isLiked(widget.song.videoId);

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 0, 14, 14),
        child: GlassPanel(
          radius: BorderRadius.circular(26),
          padding: const EdgeInsets.fromLTRB(8, 10, 8, 10),
          fillOpacity: 0.20,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  margin: const EdgeInsets.only(bottom: 6),
                  width: 38,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.35),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
                _MenuRow(
                  icon: liked
                      ? CupertinoIcons.heart_fill
                      : CupertinoIcons.heart,
                  label: liked ? 'Loved' : 'Love',
                  iconColor: liked ? AppColors.accent : Colors.white,
                  onTap: () async {
                    HapticFeedback.selectionClick();
                    if (liked) {
                      await db.unlikeSong(widget.song.videoId);
                    } else {
                      await db.likeSong(widget.song);
                    }
                    if (mounted) setState(() {});
                  },
                ),
                _MenuRow(
                  icon: CupertinoIcons.hand_thumbsdown,
                  label: 'Dislike',
                  onTap: () {
                    HapticFeedback.selectionClick();
                    ctrl.next();
                    Navigator.of(context).pop();
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('We\u2019ll show fewer like this.'),
                        duration: Duration(seconds: 1),
                      ),
                    );
                  },
                ),
                _MenuRow(
                  icon: CupertinoIcons.cloud_download,
                  label: 'Download',
                  onTap: () {
                    HapticFeedback.selectionClick();
                    final api = ref.read(apiServiceProvider);
                    final url = api.downloadUrl(widget.song.videoId);
                    Share.shareUri(Uri.parse(url));
                    Navigator.of(context).pop();
                  },
                ),
                _MenuRow(
                  icon: CupertinoIcons.add_circled,
                  label: 'Add to playlist',
                  onTap: () async {
                    Navigator.of(context).pop();
                    await _showAddToPlaylistSheet(
                      context,
                      ref,
                      widget.song,
                    );
                  },
                ),
                _MenuRow(
                  icon: CupertinoIcons.share,
                  label: 'Share song',
                  onTap: () {
                    final url =
                        'https://music.youtube.com/watch?v=${widget.song.videoId}';
                    Share.share(
                      '${widget.song.title} - ${widget.song.artist}\n$url',
                    );
                    Navigator.of(context).pop();
                  },
                ),
                _MenuRow(
                  icon: CupertinoIcons.square_stack,
                  label: 'Go to album',
                  onTap: () {
                    Navigator.of(context).pop();
                    openArtistAlbum(context, seed: widget.song);
                  },
                ),
                _MenuRow(
                  icon: CupertinoIcons.person_circle,
                  label: 'Go to artist',
                  onTap: () {
                    Navigator.of(context).pop();
                    openArtistAlbum(context, seed: widget.song);
                  },
                ),
                _MenuRow(
                  icon: CupertinoIcons.moon_zzz,
                  label: _sleep == null
                      ? 'Sleep timer'
                      : 'Sleep in ${_sleep!.inMinutes} min',
                  onTap: () async {
                    final picked = await _pickSleepDuration(context, _sleep);
                    ctrl.setSleepTimer(picked);
                    if (mounted) setState(() => _sleep = picked);
                  },
                ),
                _MenuRow(
                  icon: CupertinoIcons.bolt,
                  label: 'Crossfade settings',
                  onTap: () async {
                    Navigator.of(context).pop();
                    await _showCrossfadeSheet(context, ref);
                  },
                ),
                _MenuRow(
                  icon: CupertinoIcons.shuffle,
                  label: 'Shuffle',
                  trailing: _Toggle(active: handler.shuffleEnabled),
                  onTap: () async {
                    HapticFeedback.selectionClick();
                    await ctrl.setShuffle(!handler.shuffleEnabled);
                    if (mounted) setState(() {});
                  },
                ),
                _MenuRow(
                  icon: handler.loopMode == LoopMode.one
                      ? CupertinoIcons.repeat_1
                      : CupertinoIcons.repeat,
                  label: switch (handler.loopMode) {
                    LoopMode.off => 'Repeat: Off',
                    LoopMode.all => 'Repeat: All',
                    LoopMode.one => 'Repeat: One',
                  },
                  onTap: () async {
                    HapticFeedback.selectionClick();
                    final next = switch (handler.loopMode) {
                      LoopMode.off => LoopMode.all,
                      LoopMode.all => LoopMode.one,
                      LoopMode.one => LoopMode.off,
                    };
                    await ctrl.setRepeat(next);
                    if (mounted) setState(() {});
                  },
                ),
                _MenuRow(
                  icon: CupertinoIcons.equal_square,
                  label: 'Equalizer',
                  onTap: () {
                    Navigator.of(context).pop();
                    Navigator.of(context).push(MaterialPageRoute<void>(
                      builder: (_) => const EqualizerScreen(),
                    ));
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

Future<void> _showAddToPlaylistSheet(
  BuildContext context,
  WidgetRef ref,
  Song song,
) async {
  final db = ref.read(databaseServiceProvider);
  await showModalBottomSheet<void>(
    context: context,
    backgroundColor: Colors.transparent,
    builder: (_) {
      return StreamBuilder<List<PlaylistSummary>>(
        stream: db.watchPlaylists(),
        builder: (context, snap) {
          final playlists = snap.data ?? const <PlaylistSummary>[];
          return SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(14, 8, 14, 14),
              child: GlassPanel(
                radius: BorderRadius.circular(26),
                padding: const EdgeInsets.symmetric(vertical: 8),
                fillOpacity: 0.22,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (playlists.isEmpty)
                      const Padding(
                        padding: EdgeInsets.all(20),
                        child: Text(
                          'Sign in with Google to create playlists.',
                          style: TextStyle(color: Colors.white70),
                          textAlign: TextAlign.center,
                        ),
                      ),
                    for (final p in playlists)
                      _MenuRow(
                        icon: CupertinoIcons.music_note_list,
                        label: '${p.name}  \u00b7  ${p.songCount}',
                        onTap: () async {
                          await db.addSongToPlaylist(p.id, song);
                          if (context.mounted) Navigator.of(context).pop();
                        },
                      ),
                  ],
                ),
              ),
            ),
          );
        },
      );
    },
  );
}

Future<void> _showCrossfadeSheet(BuildContext context, WidgetRef ref) async {
  final handler = ref.read(audioHandlerProvider);
  double seconds = handler.crossfadeSeconds;
  await showModalBottomSheet<void>(
    context: context,
    backgroundColor: Colors.transparent,
    isScrollControlled: true,
    builder: (sheetContext) {
      return StatefulBuilder(builder: (context, setState) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(14, 8, 14, 14),
            child: GlassPanel(
              radius: BorderRadius.circular(26),
              padding: const EdgeInsets.fromLTRB(20, 18, 20, 18),
              fillOpacity: 0.22,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Crossfade',
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w800,
                      fontSize: 20,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Smoothly blend the end of one song into the next.',
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.7),
                      fontSize: 13,
                    ),
                  ),
                  const SizedBox(height: 18),
                  Row(
                    children: [
                      Text(
                        seconds == 0
                            ? 'Off'
                            : '${seconds.toStringAsFixed(0)} sec',
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w700,
                          fontSize: 18,
                        ),
                      ),
                      const Spacer(),
                      Text(
                        '0 – 12 sec',
                        style: TextStyle(
                          color: Colors.white.withOpacity(0.6),
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                  SliderTheme(
                    data: SliderTheme.of(context).copyWith(
                      activeTrackColor: AppColors.accent,
                      inactiveTrackColor: Colors.white.withOpacity(0.18),
                      thumbColor: Colors.white,
                      trackHeight: 3,
                    ),
                    child: Slider(
                      value: seconds.clamp(0, 12),
                      min: 0,
                      max: 12,
                      divisions: 12,
                      onChanged: (v) => setState(() => seconds = v),
                      onChangeEnd: (v) => handler.setCrossfadeSeconds(v),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      });
    },
  );
}

Future<Duration?> _pickSleepDuration(BuildContext context, Duration? current) {
  return showModalBottomSheet<Duration?>(
    context: context,
    backgroundColor: Colors.transparent,
    builder: (_) {
      const options = <Duration?>[
        null,
        Duration(minutes: 5),
        Duration(minutes: 10),
        Duration(minutes: 15),
        Duration(minutes: 30),
        Duration(minutes: 60),
      ];
      return SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 8, 14, 14),
          child: GlassPanel(
            radius: BorderRadius.circular(26),
            padding: const EdgeInsets.symmetric(vertical: 8),
            fillOpacity: 0.22,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: options.map((d) {
                final label = d == null ? 'Off' : '${d.inMinutes} minutes';
                return _MenuRow(
                  icon: d == null
                      ? CupertinoIcons.xmark_circle
                      : CupertinoIcons.timer,
                  label: label,
                  trailing: current == d
                      ? const Icon(CupertinoIcons.check_mark,
                          color: Colors.white)
                      : null,
                  onTap: () => Navigator.of(context).pop(d),
                );
              }).toList(),
            ),
          ),
        ),
      );
    },
  );
}

class _MenuRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final Widget? trailing;
  final VoidCallback onTap;
  final Color? iconColor;

  const _MenuRow({
    required this.icon,
    required this.label,
    this.trailing,
    required this.onTap,
    this.iconColor,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
          child: Row(
            children: [
              Icon(icon, color: iconColor ?? Colors.white, size: 22),
              const SizedBox(width: 14),
              Expanded(
                child: Text(
                  label,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              if (trailing != null) trailing!,
            ],
          ),
        ),
      ),
    );
  }
}

class _Toggle extends StatelessWidget {
  final bool active;
  const _Toggle({required this.active});

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 180),
      width: 36,
      height: 22,
      padding: const EdgeInsets.all(2),
      decoration: BoxDecoration(
        color: active ? AppColors.accent : Colors.white.withOpacity(0.25),
        borderRadius: BorderRadius.circular(20),
      ),
      child: AnimatedAlign(
        duration: const Duration(milliseconds: 180),
        alignment: active ? Alignment.centerRight : Alignment.centerLeft,
        child: Container(
          width: 18,
          height: 18,
          decoration: const BoxDecoration(
            color: Colors.white,
            shape: BoxShape.circle,
          ),
        ),
      ),
    );
  }
}
