import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../services/audio_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';

/// Drag-to-reorder queue management screen. Shows the live queue from
/// [queueSnapshotProvider]; tapping a row jumps to that index, the drag
/// handle reorders, swipe-to-dismiss removes.
class QueueScreen extends ConsumerWidget {
  const QueueScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final snap = ref.watch(queueSnapshotProvider).value;
    final songs = snap?.songs ?? const <Song>[];
    final current = snap?.currentIndex ?? -1;

    return Scaffold(
      appBar: AppBar(
        backgroundColor: theme.scaffoldBackgroundColor,
        title: const Text('Queue'),
      ),
      body: songs.isEmpty
          ? Center(
              child: Text(
                'Queue is empty.\nPlay a song to start one.',
                textAlign: TextAlign.center,
                style: TextStyle(color: fg.withOpacity(0.7)),
              ),
            )
          : ReorderableListView.builder(
              padding: const EdgeInsets.fromLTRB(8, 8, 8, 120),
              itemCount: songs.length,
              onReorder: (oldIndex, newIndex) {
                ref
                    .read(playerControllerProvider)
                    .reorderQueue(oldIndex, newIndex);
              },
              itemBuilder: (context, index) {
                final s = songs[index];
                final isCurrent = index == current;
                return Dismissible(
                  key: ValueKey('queue-${s.videoId}-$index'),
                  direction: DismissDirection.endToStart,
                  background: Container(
                    color: AppColors.accent.withOpacity(0.7),
                    alignment: Alignment.centerRight,
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: const Icon(Icons.delete, color: Colors.white),
                  ),
                  onDismissed: (_) =>
                      ref.read(playerControllerProvider).removeFromQueue(index),
                  child: ListTile(
                    onTap: () =>
                        ref.read(playerControllerProvider).jumpToQueueIndex(index),
                    leading: ClipRRect(
                      borderRadius: BorderRadius.circular(6),
                      child: SizedBox(
                        width: 44,
                        height: 44,
                        child: s.thumbnail.isEmpty
                            ? Container(
                                color: theme.cardColor,
                                child: Icon(
                                  Icons.music_note,
                                  color: fg.withOpacity(0.5),
                                ),
                              )
                            : CachedNetworkImage(
                                imageUrl: s.thumbnail,
                                fit: BoxFit.cover,
                                memCacheWidth: 200,
                                memCacheHeight: 200,
                              ),
                      ),
                    ),
                    title: Text(
                      s.title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: isCurrent ? AppColors.accent : fg,
                        fontWeight:
                            isCurrent ? FontWeight.w700 : FontWeight.w500,
                      ),
                    ),
                    subtitle: Text(
                      s.artist,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(color: fg.withOpacity(0.6)),
                    ),
                    trailing: ReorderableDragStartListener(
                      index: index,
                      child: Icon(
                        Icons.drag_handle,
                        color: fg.withOpacity(0.5),
                      ),
                    ),
                  ),
                );
              },
            ),
    );
  }
}
