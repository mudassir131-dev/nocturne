import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../services/api_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import '../widgets/album_card.dart';
import '../widgets/shimmer_box.dart';
import '../widgets/song_tile.dart';
import 'album_screen.dart';

/// Apple-Music-style "New" tab — surfaces fresh releases via curated search
/// queries against the existing search backend.
final _newReleasesProvider = FutureProvider<List<Song>>((ref) async {
  final api = ref.read(apiServiceProvider);
  try {
    return await api.search('new releases 2025');
  } catch (_) {
    return const <Song>[];
  }
});

final _trendingProvider = FutureProvider<List<Song>>((ref) async {
  final api = ref.read(apiServiceProvider);
  try {
    return await api.search('trending music now');
  } catch (_) {
    return const <Song>[];
  }
});

final _hindiProvider = FutureProvider<List<Song>>((ref) async {
  final api = ref.read(apiServiceProvider);
  try {
    return await api.search('new hindi songs 2025');
  } catch (_) {
    return const <Song>[];
  }
});

class NewScreen extends ConsumerWidget {
  const NewScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final newRel = ref.watch(_newReleasesProvider);
    final trending = ref.watch(_trendingProvider);
    final hindi = ref.watch(_hindiProvider);

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.only(bottom: 200),
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
            child: Text(
              'New',
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSurface,
                fontWeight: FontWeight.w800,
                fontSize: 32,
              ),
            ),
          ),
          const _SectionHeader(title: 'New Releases'),
          _CardRail(songsAsync: newRel),
          const _SectionHeader(title: 'Trending Now'),
          _CardRail(songsAsync: trending),
          const _SectionHeader(title: 'Hindi New Hits'),
          newRel.when(
            loading: () => const ShimmerSongList(count: 4),
            error: (_, __) => const SizedBox.shrink(),
            data: (_) => _Tiles(songsAsync: hindi, ref: ref),
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
        style: TextStyle(
          color: Theme.of(context).colorScheme.onSurface,
          fontSize: 20,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _CardRail extends StatelessWidget {
  final AsyncValue<List<Song>> songsAsync;
  const _CardRail({required this.songsAsync});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 200,
      child: songsAsync.when(
        loading: () => const ShimmerHorizontalCards(),
        error: (_, __) => const SizedBox.shrink(),
        data: (songs) => ListView.separated(
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
              onTap: () => openArtistAlbum(context, seed: s),
            );
          },
        ),
      ),
    );
  }
}

class _Tiles extends StatelessWidget {
  final AsyncValue<List<Song>> songsAsync;
  final WidgetRef ref;
  const _Tiles({required this.songsAsync, required this.ref});

  @override
  Widget build(BuildContext context) {
    return songsAsync.when(
      loading: () => const ShimmerSongList(count: 5),
      error: (e, _) => Padding(
        padding: const EdgeInsets.all(20),
        child: Text(
          'Could not load.\n$e',
          style: const TextStyle(color: AppColors.textSecondary),
        ),
      ),
      data: (songs) => Column(
        children: [
          for (final s in songs)
            SongTile(
              song: s,
              onTap: () => ref
                  .read(playerControllerProvider)
                  .playQueue(songs, startIndex: songs.indexOf(s)),
            ),
        ],
      ),
    );
  }
}
