import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:share_plus/share_plus.dart';

import '../models/song.dart';
import '../screens/lyrics_screen.dart';
import '../screens/queue_screen.dart';
import '../services/download_service.dart';
import '../services/settings_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';

/// Long-press / 3-dot menu for songs. Surfaces all the cross-cutting
/// actions: add-to-queue, play-next, share, download, lyrics, smart
/// shuffle, view full queue.
Future<void> showSongActions(
  BuildContext context,
  WidgetRef ref,
  Song song,
) async {
  final theme = Theme.of(context);
  final fg = theme.colorScheme.onSurface;
  await showModalBottomSheet<void>(
    context: context,
    backgroundColor: Colors.transparent,
    builder: (ctx) => BackdropFilter(
      filter: ImageFilter.blur(sigmaX: 25, sigmaY: 25),
      child: Container(
        decoration: BoxDecoration(
          color: theme.cardColor.withOpacity(0.85),
          borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
          border: Border.all(color: fg.withOpacity(0.06)),
        ),
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 44,
              height: 4,
              decoration: BoxDecoration(
                color: fg.withOpacity(0.18),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 12),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          song.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            color: fg,
                            fontWeight: FontWeight.w700,
                            fontSize: 16,
                          ),
                        ),
                        Text(
                          song.artist,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(color: fg.withOpacity(0.7)),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 8),
            _ActionTile(
              icon: Icons.queue_music,
              label: 'Play next',
              onTap: () {
                Navigator.of(ctx).pop();
                HapticFeedback.selectionClick();
                ref.read(playerControllerProvider).playNext(song);
              },
            ),
            _ActionTile(
              icon: Icons.playlist_add,
              label: 'Add to queue',
              onTap: () {
                Navigator.of(ctx).pop();
                HapticFeedback.selectionClick();
                ref.read(playerControllerProvider).addToQueue(song);
              },
            ),
            _ActionTile(
              icon: Icons.shuffle_on,
              label: 'Smart shuffle similar',
              onTap: () async {
                Navigator.of(ctx).pop();
                HapticFeedback.selectionClick();
                final added = await ref
                    .read(playerControllerProvider)
                    .smartShuffle(song);
                if (!context.mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(added > 0
                        ? 'Smart queue: $added tracks'
                        : 'Could not build smart queue.'),
                  ),
                );
              },
            ),
            _ActionTile(
              icon: Icons.download_outlined,
              label: 'Download offline',
              onTap: () async {
                Navigator.of(ctx).pop();
                final svc = ref.read(downloadServiceProvider);
                final bitrate = SettingsService.instance.downloadQualityKbps;
                if (svc.isDownloaded(song.videoId)) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Already downloaded.'),
                    ),
                  );
                  return;
                }
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Downloading at ${bitrate}kbps…')),
                );
                final result = await svc.download(song, bitrate: bitrate);
                if (!context.mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(result == null
                        ? 'Download failed.'
                        : 'Saved for offline.'),
                  ),
                );
              },
            ),
            _ActionTile(
              icon: Icons.lyrics_outlined,
              label: 'View lyrics',
              onTap: () {
                Navigator.of(ctx).pop();
                Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => LyricsScreen(song: song),
                ));
              },
            ),
            _ActionTile(
              icon: Icons.share_outlined,
              label: 'Share',
              onTap: () {
                Navigator.of(ctx).pop();
                final url = 'https://youtu.be/${song.videoId}';
                Share.share(
                  '${song.title} — ${song.artist}\n$url',
                  subject: song.title,
                );
              },
            ),
            _ActionTile(
              icon: Icons.list,
              label: 'Show queue',
              onTap: () {
                Navigator.of(ctx).pop();
                Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => const QueueScreen(),
                ));
              },
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    ),
  );
}

class _ActionTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  const _ActionTile({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final fg = Theme.of(context).colorScheme.onSurface;
    return ListTile(
      onTap: onTap,
      leading: Icon(icon, color: AppColors.accent),
      title: Text(label, style: TextStyle(color: fg)),
    );
  }
}
