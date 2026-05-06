import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/database_service.dart';
import '../utils/theme.dart';
import 'liked_screen.dart';

class LibraryScreen extends ConsumerWidget {
  const LibraryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final db = ref.watch(databaseServiceProvider);
    final likedCount = db.likedSongsLocal().length;
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 180),
        children: [
          Text(
            'Your Library',
            style: TextStyle(
              color: fg,
              fontWeight: FontWeight.w800,
              fontSize: 28,
            ),
          ),
          const SizedBox(height: 16),
          _LikedSongsCard(count: likedCount),
          const SizedBox(height: 24),
          const _SectionHeader(title: 'Playlists'),
          StreamBuilder<List<PlaylistSummary>>(
            stream: db.watchPlaylists(),
            builder: (context, snap) {
              final playlists = snap.data ?? const <PlaylistSummary>[];
              return GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                gridDelegate:
                    const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  mainAxisSpacing: 12,
                  crossAxisSpacing: 12,
                  childAspectRatio: 0.85,
                ),
                itemCount: playlists.length + 1,
                itemBuilder: (context, i) {
                  if (i == 0) {
                    return _CreatePlaylistTile(onTap: () {
                      _showCreateDialog(context, ref);
                    });
                  }
                  final p = playlists[i - 1];
                  return _PlaylistCard(playlist: p);
                },
              );
            },
          ),
          const SizedBox(height: 24),
          const _SectionHeader(title: 'Albums'),
          const _EmptyHint(text: 'Albums you save will appear here.'),
          const SizedBox(height: 24),
          const _SectionHeader(title: 'Artists'),
          const _EmptyHint(text: 'Artists you follow will appear here.'),
        ],
      ),
    );
  }

  void _showCreateDialog(BuildContext context, WidgetRef ref) {
    final controller = TextEditingController();
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: theme.cardColor,
        title: Text(
          'New playlist',
          style: TextStyle(color: fg),
        ),
        content: TextField(
          controller: controller,
          autofocus: true,
          style: TextStyle(color: fg),
          decoration: InputDecoration(
            hintText: 'Playlist name',
            hintStyle: TextStyle(color: theme.hintColor),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () async {
              final name = controller.text.trim();
              if (name.isEmpty) {
                Navigator.of(ctx).pop();
                return;
              }
              final id = await ref
                  .read(databaseServiceProvider)
                  .createPlaylist(name);
              if (!ctx.mounted) return;
              Navigator.of(ctx).pop();
              if (id == null) {
                ScaffoldMessenger.of(ctx).showSnackBar(
                  const SnackBar(
                    content: Text(
                      'Sign in with Google to sync playlists.',
                    ),
                  ),
                );
              }
            },
            child: const Text(
              'Create',
              style: TextStyle(color: AppColors.accent),
            ),
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
      padding: const EdgeInsets.symmetric(vertical: 8),
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

class _LikedSongsCard extends StatelessWidget {
  final int count;
  const _LikedSongsCard({required this.count});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(AppRadius.card),
      onTap: () => Navigator.of(context).push(
        MaterialPageRoute<void>(builder: (_) => const LikedScreen()),
      ),
      child: Container(
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            colors: [Color(0xFFB71C1C), Color(0xFFE53935)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(AppRadius.card),
        ),
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            const Icon(Icons.favorite, color: Colors.white, size: 36),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Liked Songs',
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w700,
                      fontSize: 18,
                    ),
                  ),
                  Text(
                    '$count song${count == 1 ? '' : 's'}',
                    style: const TextStyle(color: Colors.white70),
                  ),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: Colors.white70),
          ],
        ),
      ),
    );
  }
}

class _CreatePlaylistTile extends StatelessWidget {
  final VoidCallback onTap;
  const _CreatePlaylistTile({required this.onTap});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    return InkWell(
      borderRadius: BorderRadius.circular(AppRadius.card),
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: theme.cardColor,
          borderRadius: BorderRadius.circular(AppRadius.card),
          border: Border.all(color: fg.withOpacity(0.10)),
        ),
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.add, color: AppColors.accent, size: 36),
              const SizedBox(height: 8),
              Text(
                'New Playlist',
                style: TextStyle(color: fg),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PlaylistCard extends StatelessWidget {
  final PlaylistSummary playlist;
  const _PlaylistCard({required this.playlist});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    return Container(
      decoration: BoxDecoration(
        color: theme.cardColor,
        borderRadius: BorderRadius.circular(AppRadius.card),
      ),
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: Container(
                color: fg.withOpacity(0.08),
                child: Center(
                  child: Icon(
                    Icons.queue_music,
                    color: fg.withOpacity(0.5),
                    size: 56,
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            playlist.name,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              color: fg,
              fontWeight: FontWeight.w600,
            ),
          ),
          Text(
            '${playlist.songCount} songs',
            style: TextStyle(color: theme.hintColor),
          ),
        ],
      ),
    );
  }
}

class _EmptyHint extends StatelessWidget {
  final String text;
  const _EmptyHint({required this.text});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Text(
        text,
        style: TextStyle(color: Theme.of(context).hintColor),
      ),
    );
  }
}
