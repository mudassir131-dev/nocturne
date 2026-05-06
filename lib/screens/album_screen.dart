import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import '../widgets/song_tile.dart';

/// Generic album/playlist detail screen showing a header art and track list.
class AlbumScreen extends ConsumerWidget {
  final String title;
  final String subtitle;
  final String coverUrl;
  final List<Song> songs;

  const AlbumScreen({
    super.key,
    required this.title,
    required this.subtitle,
    required this.coverUrl,
    required this.songs,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 320,
            pinned: true,
            backgroundColor: AppColors.background,
            flexibleSpace: FlexibleSpaceBar(
              title: Text(title, style: const TextStyle(fontSize: 16)),
              background: _Header(
                title: title,
                subtitle: subtitle,
                coverUrl: coverUrl,
              ),
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.accent,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(30),
                  ),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 24,
                    vertical: 14,
                  ),
                ),
                icon: const Icon(Icons.play_arrow),
                label: const Text(
                  'Play All',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                onPressed: songs.isEmpty
                    ? null
                    : () => ref
                        .read(playerControllerProvider)
                        .playQueue(songs),
              ),
            ),
          ),
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
          const SliverToBoxAdapter(child: SizedBox(height: 180)),
        ],
      ),
    );
  }
}

class _Header extends StatelessWidget {
  final String title;
  final String subtitle;
  final String coverUrl;

  const _Header({
    required this.title,
    required this.subtitle,
    required this.coverUrl,
  });

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        if (coverUrl.isNotEmpty)
          CachedNetworkImage(imageUrl: coverUrl, fit: BoxFit.cover)
        else
          Container(color: AppColors.card),
        Container(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              colors: [Colors.transparent, Colors.black],
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
            ),
          ),
        ),
        Align(
          alignment: Alignment.bottomLeft,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 56),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w800,
                    fontSize: 24,
                  ),
                ),
                if (subtitle.isNotEmpty)
                  Text(
                    subtitle,
                    style: const TextStyle(color: Colors.white70),
                  ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
