import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../services/api_service.dart';
import '../services/database_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import '../widgets/album_card.dart';
import '../widgets/song_tile.dart';
import 'album_screen.dart';

/// Home-screen recommendations sourced from a default search query so the
/// feed always has content, even before the user has any history.
final recommendedProvider = FutureProvider<List<Song>>((ref) async {
  final api = ref.read(apiServiceProvider);
  try {
    return await api.search('top music hits');
  } catch (_) {
    return const <Song>[];
  }
});

/// "Heavy Rotation" feed — most-listened tracks (proxied by another seed
/// search so something always shows up before personalisation is wired).
final heavyRotationProvider = FutureProvider<List<Song>>((ref) async {
  final api = ref.read(apiServiceProvider);
  try {
    return await api.search('top music charts global');
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

  Future<void> _refresh(WidgetRef ref) async {
    ref.invalidate(recommendedProvider);
    ref.invalidate(heavyRotationProvider);
    // Wait for both to settle so the spinner stays up while loading.
    await Future.wait([
      ref.read(recommendedProvider.future),
      ref.read(heavyRotationProvider.future),
    ]);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final db = ref.watch(databaseServiceProvider);
    final recently = db.recentlyPlayedLocal();
    final recsAsync = ref.watch(recommendedProvider);
    final heavyAsync = ref.watch(heavyRotationProvider);
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;

    return SafeArea(
      child: RefreshIndicator(
        color: AppColors.accent,
        backgroundColor: theme.cardColor,
        onRefresh: () => _refresh(ref),
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(
            parent: BouncingScrollPhysics(),
          ),
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
                          style: TextStyle(
                            color: theme.hintColor,
                            fontSize: 14,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'Nocturne',
                          style: TextStyle(
                            color: fg,
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
                      onTap: () => openArtistAlbum(context, seed: s),
                    );
                  },
                ),
              ),
            ],
            const _SectionHeader(title: 'Recommended For You'),
            recsAsync.when(
              loading: () => const _ListShimmer(count: 4),
              error: (e, _) => _ErrorBox(message: 'Couldn\'t load: $e'),
              data: (songs) {
                if (songs.isEmpty) {
                  return Padding(
                    padding: const EdgeInsets.all(20),
                    child: Text(
                      'Connect your backend to see recommendations.',
                      style: TextStyle(color: theme.hintColor),
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
            const _SectionHeader(title: 'Heavy Rotation'),
            SizedBox(
              height: 200,
              child: heavyAsync.when(
                loading: () => const _CardsShimmer(count: 5),
                error: (_, __) => const SizedBox.shrink(),
                data: (songs) {
                  if (songs.isEmpty) return const SizedBox.shrink();
                  return ListView.separated(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    itemCount: songs.length,
                    separatorBuilder: (_, __) => const SizedBox(width: 12),
                    itemBuilder: (_, i) {
                      final s = songs[i];
                      return AlbumCard(
                        title: s.title,
                        subtitle: s.artist,
                        imageUrl: s.thumbnail,
                        onTap: () => ref
                            .read(playerControllerProvider)
                            .playQueue(songs, startIndex: i),
                      );
                    },
                  );
                },
              ),
            ),
          ],
        ),
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
        style: TextStyle(
          color: Theme.of(context).colorScheme.onSurface,
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
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    return Container(
      width: 40,
      height: 40,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: theme.cardColor,
        border: Border.all(color: fg.withOpacity(0.18)),
      ),
      child: Icon(CupertinoIcons.person_fill, color: fg.withOpacity(0.7)),
    );
  }
}

class _ListShimmer extends StatelessWidget {
  final int count;
  const _ListShimmer({required this.count});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: List.generate(
        count,
        (_) => Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            children: [
              const _ShimmerBlock(width: 50, height: 50, radius: 8),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    _ShimmerBlock(height: 12, radius: 4),
                    SizedBox(height: 8),
                    _ShimmerBlock(width: 120, height: 10, radius: 4),
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

class _CardsShimmer extends StatelessWidget {
  final int count;
  const _CardsShimmer({required this.count});

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 20),
      itemCount: count,
      separatorBuilder: (_, __) => const SizedBox(width: 12),
      itemBuilder: (_, __) {
        return const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _ShimmerBlock(width: 140, height: 140, radius: 12),
            SizedBox(height: 8),
            _ShimmerBlock(width: 120, height: 12, radius: 4),
            SizedBox(height: 6),
            _ShimmerBlock(width: 80, height: 10, radius: 4),
          ],
        );
      },
    );
  }
}

class _ShimmerBlock extends StatefulWidget {
  final double? width;
  final double height;
  final double radius;
  const _ShimmerBlock({
    this.width,
    required this.height,
    this.radius = 4,
  });

  @override
  State<_ShimmerBlock> createState() => _ShimmerBlockState();
}

class _ShimmerBlockState extends State<_ShimmerBlock>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1300),
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final base = AppColors.card;
    final highlight = Color.alphaBlend(
      Colors.white.withOpacity(0.06),
      base,
    );
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        final t = _controller.value;
        return Container(
          width: widget.width ?? double.infinity,
          height: widget.height,
          decoration: BoxDecoration(
            color: Color.lerp(base, highlight, t),
            borderRadius: BorderRadius.circular(widget.radius),
          ),
        );
      },
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
