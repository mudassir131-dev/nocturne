import 'dart:ui';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../services/api_service.dart';
import '../state/player_provider.dart';
import '../widgets/song_tile.dart';

/// Album / playlist / artist detail screen. Renders a large header with
/// a blurred backdrop and the cover art, Play / Shuffle action chips, and
/// the track list.
///
/// Songs can be supplied directly ([songs]) or loaded async via
/// [songsLoader]. The loader is preferred for "More from {artist}" style
/// flows where we want to fetch related tracks lazily.
class AlbumScreen extends ConsumerStatefulWidget {
  final String title;
  final String subtitle;
  final String coverUrl;
  final List<Song>? songs;
  final Future<List<Song>> Function()? songsLoader;
  final List<Color>? gradientColors;

  const AlbumScreen({
    super.key,
    required this.title,
    required this.subtitle,
    this.coverUrl = '',
    this.songs,
    this.songsLoader,
    this.gradientColors,
  }) : assert(songs != null || songsLoader != null,
            'AlbumScreen needs either songs or songsLoader');

  @override
  ConsumerState<AlbumScreen> createState() => _AlbumScreenState();
}

class _AlbumScreenState extends ConsumerState<AlbumScreen> {
  late Future<List<Song>> _future;

  @override
  void initState() {
    super.initState();
    _future = widget.songs != null
        ? Future.value(widget.songs!)
        : widget.songsLoader!();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final accent = theme.colorScheme.primary;
    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      body: FutureBuilder<List<Song>>(
        future: _future,
        builder: (context, snap) {
          final songs = snap.data ?? const <Song>[];
          final loading = snap.connectionState == ConnectionState.waiting;
          return CustomScrollView(
            slivers: [
              SliverAppBar(
                expandedHeight: 340,
                pinned: true,
                stretch: true,
                backgroundColor: theme.scaffoldBackgroundColor,
                iconTheme: IconThemeData(color: theme.colorScheme.onSurface),
                flexibleSpace: FlexibleSpaceBar(
                  collapseMode: CollapseMode.parallax,
                  stretchModes: const [
                    StretchMode.zoomBackground,
                    StretchMode.fadeTitle,
                  ],
                  title: Text(
                    widget.title,
                    style: TextStyle(
                      fontSize: 16,
                      color: theme.colorScheme.onSurface,
                    ),
                  ),
                  background: _Header(
                    title: widget.title,
                    subtitle: widget.subtitle,
                    coverUrl: widget.coverUrl,
                    gradientColors: widget.gradientColors,
                  ),
                ),
              ),
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
                  child: Row(
                    children: [
                      Expanded(
                        child: _PrimaryActionButton(
                          icon: Icons.play_arrow,
                          label: 'Play',
                          color: accent,
                          enabled: songs.isNotEmpty,
                          onPressed: () => ref
                              .read(playerControllerProvider)
                              .playQueue(songs),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: _PrimaryActionButton(
                          icon: Icons.shuffle,
                          label: 'Shuffle',
                          color: accent,
                          outlined: true,
                          enabled: songs.isNotEmpty,
                          onPressed: () async {
                            final ctrl = ref.read(playerControllerProvider);
                            await ctrl.setShuffle(true);
                            await ctrl.playQueue(songs);
                          },
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              if (loading)
                const SliverFillRemaining(
                  hasScrollBody: false,
                  child: Center(
                    child: Padding(
                      padding: EdgeInsets.only(top: 40),
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                  ),
                )
              else if (songs.isEmpty)
                SliverFillRemaining(
                  hasScrollBody: false,
                  child: Center(
                    child: Padding(
                      padding: const EdgeInsets.all(32),
                      child: Text(
                        'No tracks here yet.',
                        style: TextStyle(color: theme.hintColor),
                      ),
                    ),
                  ),
                )
              else
                SliverList.builder(
                  itemCount: songs.length,
                  itemBuilder: (_, i) {
                    final s = songs[i];
                    return SongTile(
                      song: s,
                      onTap: () => ref
                          .read(playerControllerProvider)
                          .playQueue(songs, startIndex: i),
                    );
                  },
                ),
              const SliverToBoxAdapter(child: SizedBox(height: 200)),
            ],
          );
        },
      ),
    );
  }
}

class _PrimaryActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final bool outlined;
  final bool enabled;
  final VoidCallback onPressed;

  const _PrimaryActionButton({
    required this.icon,
    required this.label,
    required this.color,
    required this.onPressed,
    this.outlined = false,
    this.enabled = true,
  });

  @override
  Widget build(BuildContext context) {
    if (outlined) {
      return OutlinedButton.icon(
        onPressed: enabled ? onPressed : null,
        icon: Icon(icon, color: color),
        label: Text(label, style: TextStyle(color: color)),
        style: OutlinedButton.styleFrom(
          padding: const EdgeInsets.symmetric(vertical: 14),
          side: BorderSide(color: color, width: 1.5),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(30),
          ),
        ),
      );
    }
    return ElevatedButton.icon(
      onPressed: enabled ? onPressed : null,
      icon: Icon(icon, color: Colors.white),
      label: Text(
        label,
        style: const TextStyle(
          color: Colors.white,
          fontWeight: FontWeight.w700,
        ),
      ),
      style: ElevatedButton.styleFrom(
        backgroundColor: color,
        padding: const EdgeInsets.symmetric(vertical: 14),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(30),
        ),
      ),
    );
  }
}

class _Header extends StatelessWidget {
  final String title;
  final String subtitle;
  final String coverUrl;
  final List<Color>? gradientColors;

  const _Header({
    required this.title,
    required this.subtitle,
    required this.coverUrl,
    this.gradientColors,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fallback = gradientColors ??
        [theme.colorScheme.primary, theme.colorScheme.surface];
    return Stack(
      fit: StackFit.expand,
      children: [
        // Backdrop: blurred album art, or gradient when no art is provided.
        if (coverUrl.isNotEmpty)
          ImageFiltered(
            imageFilter: ImageFilter.blur(sigmaX: 30, sigmaY: 30),
            child: CachedNetworkImage(
              imageUrl: coverUrl,
              fit: BoxFit.cover,
              memCacheWidth: 720,
              memCacheHeight: 720,
            ),
          )
        else
          Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: fallback,
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
            ),
          ),
        Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [Colors.black.withOpacity(0.10), Colors.black],
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
            ),
          ),
        ),
        Align(
          alignment: Alignment.bottomLeft,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 56),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(16),
                  child: SizedBox(
                    width: 140,
                    height: 140,
                    child: coverUrl.isNotEmpty
                        ? CachedNetworkImage(
                            imageUrl: coverUrl,
                            fit: BoxFit.cover,
                            memCacheWidth: 480,
                            memCacheHeight: 480,
                          )
                        : Container(
                            decoration: BoxDecoration(
                              gradient: LinearGradient(
                                colors: fallback,
                                begin: Alignment.topLeft,
                                end: Alignment.bottomRight,
                              ),
                            ),
                            child: const Icon(
                              Icons.album,
                              color: Colors.white70,
                              size: 56,
                            ),
                          ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        title,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w800,
                          fontSize: 22,
                        ),
                      ),
                      if (subtitle.isNotEmpty) ...[
                        const SizedBox(height: 4),
                        Text(
                          subtitle,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(color: Colors.white70),
                        ),
                      ],
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

/// Convenience: open an album-style screen showing all songs by an
/// artist (loaded by searching the backend on demand).
Future<void> openArtistAlbum(
  BuildContext context, {
  required Song seed,
}) {
  return Navigator.of(context).push(
    PageRouteBuilder<void>(
      transitionDuration: const Duration(milliseconds: 320),
      pageBuilder: (_, __, ___) => Consumer(
        builder: (ctx, ref, _) => AlbumScreen(
          title: seed.artist.isEmpty ? seed.title : seed.artist,
          subtitle: seed.artist.isEmpty ? '' : 'More like ${seed.title}',
          coverUrl: seed.thumbnail,
          songsLoader: () async {
            final api = ref.read(apiServiceProvider);
            try {
              final query = seed.artist.isNotEmpty ? seed.artist : seed.title;
              final results = await api.search(query);
              if (results.isEmpty) return [seed];
              // Make sure the seed song appears first if not already in results.
              final hasSeed = results.any((r) => r.videoId == seed.videoId);
              return hasSeed ? results : [seed, ...results];
            } catch (_) {
              return [seed];
            }
          },
        ),
      ),
      transitionsBuilder: (_, anim, __, child) => FadeTransition(
        opacity: CurvedAnimation(parent: anim, curve: Curves.easeOut),
        child: child,
      ),
    ),
  );
}
