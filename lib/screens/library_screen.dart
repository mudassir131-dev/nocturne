import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../services/database_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import '../widgets/album_card.dart';
import 'liked_screen.dart';

/// Main library hub: pinned items grid + red-icon list rows + Recently
/// Added carousel.
class LibraryScreen extends ConsumerWidget {
  const LibraryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final db = ref.watch(databaseServiceProvider);
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final liked = db.likedSongsLocal();
    final recent = db.recentlyPlayedLocal();

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 180),
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  'Library',
                  style: TextStyle(
                    color: fg,
                    fontWeight: FontWeight.w800,
                    fontSize: 32,
                  ),
                ),
              ),
              const _ProfileAvatar(),
              IconButton(
                icon: Icon(CupertinoIcons.ellipsis, color: fg),
                onPressed: () => _showLibraryMenu(context),
              ),
            ],
          ),
          const SizedBox(height: 16),
          _PinnedGrid(liked: liked),
          const SizedBox(height: 24),
          StreamBuilder<List<PlaylistSummary>>(
            stream: db.watchPlaylists(),
            builder: (context, snap) {
              final playlists = snap.data ?? const <PlaylistSummary>[];
              return Column(
                children: [
                  _LibraryRow(
                    icon: CupertinoIcons.music_note_list,
                    label: 'Playlists',
                    count: playlists.length,
                    onTap: () => _showPlaylistsSheet(context, ref, playlists),
                  ),
                  _LibraryRow(
                    icon: CupertinoIcons.music_mic,
                    label: 'Artists',
                    count: 0,
                    onTap: () {},
                  ),
                  _LibraryRow(
                    icon: CupertinoIcons.square_stack,
                    label: 'Albums',
                    count: 0,
                    onTap: () {},
                  ),
                  _LibraryRow(
                    icon: CupertinoIcons.music_note,
                    label: 'Songs',
                    count: liked.length,
                    onTap: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => const LikedScreen(),
                      ),
                    ),
                  ),
                  _LibraryRow(
                    icon: CupertinoIcons.tag_fill,
                    label: 'Genres',
                    onTap: () {},
                  ),
                  _LibraryRow(
                    icon: CupertinoIcons.cloud_download_fill,
                    label: 'Downloaded',
                    onTap: () {},
                  ),
                ],
              );
            },
          ),
          if (recent.isNotEmpty) ...[
            const SizedBox(height: 24),
            Text(
              'Recently Added',
              style: TextStyle(
                color: fg,
                fontSize: 20,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 8),
            SizedBox(
              height: 200,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: recent.length,
                separatorBuilder: (_, __) => const SizedBox(width: 12),
                itemBuilder: (_, i) {
                  final s = recent[i];
                  return AlbumCard(
                    title: s.title,
                    subtitle: s.artist,
                    imageUrl: s.thumbnail,
                    onTap: () => ref
                        .read(playerControllerProvider)
                        .playQueue(recent, startIndex: i),
                  );
                },
              ),
            ),
          ],
        ],
      ),
    );
  }

  void _showLibraryMenu(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: Theme.of(context).cardColor,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) {
        final fg = Theme.of(context).colorScheme.onSurface;
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ListTile(
                leading: const Icon(CupertinoIcons.sort_down,
                    color: AppColors.accent),
                title: Text('Sort by recent', style: TextStyle(color: fg)),
                onTap: () => Navigator.of(context).pop(),
              ),
              ListTile(
                leading: const Icon(CupertinoIcons.cloud_download,
                    color: AppColors.accent),
                title: Text('Available offline only',
                    style: TextStyle(color: fg)),
                onTap: () => Navigator.of(context).pop(),
              ),
              const SizedBox(height: 8),
            ],
          ),
        );
      },
    );
  }

  void _showPlaylistsSheet(
    BuildContext context,
    WidgetRef ref,
    List<PlaylistSummary> playlists,
  ) {
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: Theme.of(context).cardColor,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      isScrollControlled: true,
      builder: (_) {
        final fg = Theme.of(context).colorScheme.onSurface;
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Playlists',
                  style: TextStyle(
                    color: fg,
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 8),
                if (playlists.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    child: Text(
                      'Sign in with Google to create playlists.',
                      style: TextStyle(color: Theme.of(context).hintColor),
                    ),
                  )
                else
                  for (final p in playlists)
                    ListTile(
                      leading: const Icon(
                        CupertinoIcons.music_note_list,
                        color: AppColors.accent,
                      ),
                      title: Text(p.name, style: TextStyle(color: fg)),
                      subtitle: Text(
                        '${p.songCount} songs',
                        style: TextStyle(color: Theme.of(context).hintColor),
                      ),
                    ),
                TextButton.icon(
                  onPressed: () => _showCreateDialog(context, ref),
                  icon: const Icon(Icons.add, color: AppColors.accent),
                  label: const Text(
                    'New Playlist',
                    style: TextStyle(color: AppColors.accent),
                  ),
                ),
              ],
            ),
          ),
        );
      },
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
        title: Text('New playlist', style: TextStyle(color: fg)),
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
              final id =
                  await ref.read(databaseServiceProvider).createPlaylist(name);
              if (!ctx.mounted) return;
              Navigator.of(ctx).pop();
              if (id == null) {
                ScaffoldMessenger.of(ctx).showSnackBar(
                  const SnackBar(
                    content: Text('Sign in with Google to sync playlists.'),
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

/// Pinned shortcuts grid at the top of the library.
class _PinnedGrid extends StatelessWidget {
  final List<Song> liked;
  const _PinnedGrid({required this.liked});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final items = <_Pinned>[
      const _Pinned(
        icon: CupertinoIcons.heart_fill,
        label: 'Liked Songs',
        gradient: [Color(0xFFB71C1C), Color(0xFFE53935)],
      ),
      const _Pinned(
        icon: CupertinoIcons.cloud_download_fill,
        label: 'Downloaded',
        gradient: [Color(0xFF263238), Color(0xFF455A64)],
      ),
      const _Pinned(
        icon: CupertinoIcons.music_note_list,
        label: 'Playlists',
        gradient: [Color(0xFF6A1B9A), Color(0xFF8E24AA)],
      ),
      const _Pinned(
        icon: CupertinoIcons.music_mic,
        label: 'Artists',
        gradient: [Color(0xFF1E88E5), Color(0xFF26A69A)],
      ),
    ];

    // The first pinned item shows mini-thumbs from liked songs when
    // available so the grid has a personal touch.
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        mainAxisSpacing: 12,
        crossAxisSpacing: 12,
        childAspectRatio: 2.4,
      ),
      itemCount: items.length,
      itemBuilder: (_, i) {
        final p = items[i];
        return InkWell(
          borderRadius: BorderRadius.circular(AppRadius.card),
          onTap: () {
            if (i == 0) {
              Navigator.of(context).push(
                MaterialPageRoute<void>(builder: (_) => const LikedScreen()),
              );
            }
          },
          child: Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: p.gradient,
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(AppRadius.card),
            ),
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                if (i == 0 && liked.isNotEmpty)
                  ClipOval(
                    child: SizedBox(
                      width: 36,
                      height: 36,
                      child: liked.first.thumbnail.isNotEmpty
                          ? CachedNetworkImage(
                              imageUrl: liked.first.thumbnail,
                              fit: BoxFit.cover,
                            )
                          : Icon(p.icon,
                              color: Colors.white.withOpacity(0.95)),
                    ),
                  )
                else
                  Icon(p.icon, color: Colors.white, size: 28),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    p.label,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w800,
                      fontSize: 14,
                    ),
                  ),
                ),
                Icon(CupertinoIcons.chevron_right,
                    color: fg.withOpacity(0.5), size: 18),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _Pinned {
  final IconData icon;
  final String label;
  final List<Color> gradient;
  const _Pinned({
    required this.icon,
    required this.label,
    required this.gradient,
  });
}

class _LibraryRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final int? count;
  final VoidCallback onTap;
  const _LibraryRow({
    required this.icon,
    required this.label,
    this.count,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(color: fg.withOpacity(0.07)),
          ),
        ),
        child: Row(
          children: [
            Icon(icon, color: AppColors.accent, size: 24),
            const SizedBox(width: 16),
            Expanded(
              child: Text(
                label,
                style: TextStyle(
                  color: fg,
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            if (count != null && count! > 0)
              Container(
                margin: const EdgeInsets.only(right: 8),
                padding: const EdgeInsets.symmetric(
                  horizontal: 8,
                  vertical: 2,
                ),
                decoration: BoxDecoration(
                  color: AppColors.accent.withOpacity(0.18),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  '$count',
                  style: const TextStyle(
                    color: AppColors.accent,
                    fontWeight: FontWeight.w700,
                    fontSize: 12,
                  ),
                ),
              ),
            Icon(CupertinoIcons.chevron_right,
                color: fg.withOpacity(0.45), size: 18),
          ],
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
