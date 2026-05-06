import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../services/database_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import '../widgets/song_tile.dart';

class LikedScreen extends ConsumerWidget {
  const LikedScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final db = ref.watch(databaseServiceProvider);
    final localLiked = db.likedSongsLocal();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Liked Songs'),
        backgroundColor: AppColors.background,
      ),
      body: StreamBuilder<List<Song>>(
        stream: db.watchLikedSongs(),
        initialData: localLiked,
        builder: (context, snap) {
          final songs = snap.data ?? localLiked;
          if (songs.isEmpty) {
            return const Center(
              child: Padding(
                padding: EdgeInsets.all(32),
                child: Text(
                  'You haven\'t liked any songs yet.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: AppColors.textSecondary),
                ),
              ),
            );
          }
          return ListView.builder(
            padding: const EdgeInsets.only(bottom: 180),
            itemCount: songs.length,
            itemBuilder: (_, i) {
              final s = songs[i];
              return SongTile(
                song: s,
                onTap: () => ref
                    .read(playerControllerProvider)
                    .playQueue(songs, startIndex: i),
                onMenu: () async {
                  await db.unlikeSong(s.videoId);
                },
              );
            },
          );
        },
      ),
    );
  }
}
