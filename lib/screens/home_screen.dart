import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../services/api_service.dart';
import '../services/database_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import '../widgets/album_card.dart';
import '../widgets/song_tile.dart';

/// Recommended-songs feed sourced from a default search query so the home
/// screen always has something to show, even before the user has any
/// listening history.
final recommendedProvider = FutureProvider<List<Song>>((ref) async {
  final api = ref.read(apiServiceProvider);
  try {
    return await api.search('top music hits');
  } catch (_) {
    return const <Song>[];
  }
});

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  String _greeting() {
    final h = DateTime.now().hour;
    if (h < 12) return 'Good Morning';
    if (h < 18) return 'Good Afternoon';
    return 'Good Evening';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final db = ref.watch(databaseServiceProvider);
    final recently = db.recentlyPlayedLocal();
    final recsAsync = ref.watch(recommendedProvider);

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.only(bottom: 180),
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _greeting(),
                        style: const TextStyle(
                          color: AppColors.textSecondary,
                          fontSize: 14,
                        ),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Nocturne',
                        style: TextStyle(
                          color: AppColors.textPrimary,
                          fontWeight: FontWeight.w800,
                          fontSize: 28,
                        ),
                      ),
                    ],
                  ),
                ),
                const _ProfileAvatar(),
              ],
            ),
          ),
          if (recently.isNotEmpty) ...[
            const _SectionHeader(title: 'Recently Played'),
            SizedBox(
              height: 200,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 20),
                itemCount: recently.length,
                separatorBuilder: (_, __) => const SizedBox(width: 12),
                itemBuilder: (_, i) {
                  final s = recently[i];
                  return AlbumCard(
                    title: s.title,
                    subtitle: s.artist,
                    imageUrl: s.thumbnail,
                    onTap: () =>
                        ref.read(playerControllerProvider).playSong(s),
                  );
                },
              ),
            ),
          ],
          const _SectionHeader(title: 'Recommended For You'),
          recsAsync.when(
            loading: () => const _Shimmer(count: 5),
            error: (e, _) => _ErrorBox(
              message: 'Could not load recommendations.\n$e',
            ),
            data: (songs) {
              if (songs.isEmpty) {
                return const Padding(
                  padding: EdgeInsets.all(20),
                  child: Text(
                    'Connect your backend to see recommendations.',
                    style: TextStyle(color: AppColors.textSecondary),
                  ),
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
                      onAdd: () {},
                      onMenu: () {},
                    ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  const _SectionHeader({required this.title});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 12),
      child: Text(
        title,
        style: const TextStyle(
          color: AppColors.textPrimary,
          fontSize: 20,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _ProfileAvatar extends StatelessWidget {
  const _ProfileAvatar();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 40,
      height: 40,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: AppColors.card,
        border: Border.all(color: Colors.white.withOpacity(0.18)),
      ),
      child: const Icon(Icons.person, color: Colors.white70),
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
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
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
                    Container(
                      height: 10,
                      width: 120,
                      color: AppColors.card,
                    ),
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

class _ErrorBox extends StatelessWidget {
  final String message;
  const _ErrorBox({required this.message});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Text(
        message,
        style: const TextStyle(color: AppColors.textSecondary),
      ),
    );
  }
}
