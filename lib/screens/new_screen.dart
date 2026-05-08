import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../services/api_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import '../widgets/song_tile.dart';

/// `New` tab — fresh releases / what's-new feed. Backed by a yt-dlp
/// search since the backend doesn't expose a dedicated "new releases"
/// endpoint; the query is broad enough that results refresh frequently.
final _newReleasesProvider = FutureProvider.autoDispose<List<Song>>((ref) async {
  final api = ref.read(apiServiceProvider);
  try {
    return await api.search('new music releases this week');
  } catch (_) {
    return const <Song>[];
  }
});

class NewScreen extends ConsumerWidget {
  const NewScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(_newReleasesProvider);
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;

    return SafeArea(
      child: RefreshIndicator(
        color: AppColors.accent,
        onRefresh: () async {
          ref.invalidate(_newReleasesProvider);
          await ref.read(_newReleasesProvider.future);
        },
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 200),
          children: [
            Text(
              'New',
              style: TextStyle(
                color: fg,
                fontWeight: FontWeight.w800,
                fontSize: 28,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'Fresh tracks from across the web',
              style: TextStyle(color: fg.withOpacity(0.6), fontSize: 13),
            ),
            const SizedBox(height: 16),
            async.when(
              loading: () => const _Shimmer(count: 8),
              error: (e, _) => _EmptyState(
                icon: CupertinoIcons.exclamationmark_triangle,
                message: 'Could not load new releases.\n$e',
              ),
              data: (songs) {
                if (songs.isEmpty) {
                  return const _EmptyState(
                    icon: CupertinoIcons.music_note_2,
                    message: 'Connect the backend to see new releases.',
                  );
                }
                return Column(
                  children: [
                    for (final s in songs)
                      SongTile(
                        song: s,
                        onTap: () => ref
                            .read(playerControllerProvider)
                            .playQueue(songs, startIndex: songs.indexOf(s)),
                        onMenu: () {},
                      ),
                  ],
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _Shimmer extends StatelessWidget {
  final int count;
  const _Shimmer({required this.count});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: List.generate(
        count,
        (_) => Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Row(
            children: [
              Container(
                width: 50,
                height: 50,
                decoration: BoxDecoration(
                  color: AppColors.card,
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      height: 12,
                      width: double.infinity,
                      color: AppColors.card,
                    ),
                    const SizedBox(height: 6),
                    Container(height: 10, width: 120, color: AppColors.card),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  final IconData icon;
  final String message;
  const _EmptyState({required this.icon, required this.message});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 60),
      child: Column(
        children: [
          Icon(icon, color: Colors.white24, size: 56),
          const SizedBox(height: 12),
          Text(
            message,
            textAlign: TextAlign.center,
            style: const TextStyle(color: Colors.white60),
          ),
        ],
      ),
    );
  }
}
