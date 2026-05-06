import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/database_service.dart';
import 'album_screen.dart';

/// "Liked Songs" — uses the shared [AlbumScreen] layout so it gets the
/// blurred header, Play/Shuffle controls and consistent track list.
class LikedScreen extends ConsumerWidget {
  const LikedScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final db = ref.watch(databaseServiceProvider);
    final liked = db.likedSongsLocal();
    String cover = '';
    for (final s in liked) {
      if (s.thumbnail.isNotEmpty) {
        cover = s.thumbnail;
        break;
      }
    }
    return AlbumScreen(
      title: 'Liked Songs',
      subtitle: '${liked.length} song${liked.length == 1 ? '' : 's'}',
      coverUrl: cover,
      songs: liked,
      gradientColors: const [
        Color(0xFFB71C1C),
        Color(0xFFE53935),
      ],
    );
  }
}
