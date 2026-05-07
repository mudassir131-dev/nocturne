import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../services/audio_service.dart';
import '../state/player_provider.dart';

/// iOS-style "Up Next" sheet — shown above the player. Lets the user
/// reorder the queue with a long-press drag and tap-to-jump-to-track.
class QueueScreen extends ConsumerWidget {
  const QueueScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final handler = ref.watch(audioHandlerProvider);
    final songs = ref.watch(queueRevisionProvider).when(
          data: (_) => handler.songs,
          loading: () => handler.songs,
          error: (_, __) => handler.songs,
        );
    final currentIndex = handler.currentIndex;
    final upcoming = currentIndex < 0 || currentIndex >= songs.length
        ? songs
        : songs.sublist(currentIndex);

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: SafeArea(
        child: Column(
          children: [
            const _Grabber(),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 4, 12, 8),
              child: Row(
                children: [
                  const Expanded(
                    child: Text(
                      'Up Next',
                      style: TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w800,
                        fontSize: 22,
                      ),
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, color: Colors.white),
                    onPressed: () => Navigator.of(context).maybePop(),
                  ),
                ],
              ),
            ),
            Expanded(
              child: songs.isEmpty
                  ? const Center(
                      child: Text(
                        'Queue is empty.',
                        style: TextStyle(color: Colors.white70),
                      ),
                    )
                  : ReorderableListView.builder(
                      padding: const EdgeInsets.fromLTRB(8, 0, 8, 24),
                      buildDefaultDragHandles: false,
                      itemCount: upcoming.length,
                      onReorder: (oldIndex, newIndex) {
                        // ReorderableListView gives indices relative to its
                        // own list; map them back to the global queue.
                        final base = currentIndex < 0 ? 0 : currentIndex;
                        var src = base + oldIndex;
                        var dst = base + newIndex;
                        if (dst > src) dst -= 1;
                        ref.read(playerControllerProvider).reorder(src, dst);
                      },
                      itemBuilder: (context, listIndex) {
                        final globalIndex =
                            (currentIndex < 0 ? 0 : currentIndex) + listIndex;
                        final song = upcoming[listIndex];
                        final isCurrent = globalIndex == currentIndex;
                        return _QueueRow(
                          key: ValueKey('${song.videoId}-$globalIndex'),
                          song: song,
                          listIndex: listIndex,
                          isCurrent: isCurrent,
                          onTap: () async {
                            if (!isCurrent) {
                              await ref
                                  .read(playerControllerProvider)
                                  .jumpTo(globalIndex);
                            }
                          },
                          onRemove: isCurrent
                              ? null
                              : () => ref
                                  .read(playerControllerProvider)
                                  .removeAt(globalIndex),
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

class _Grabber extends StatelessWidget {
  const _Grabber();

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      width: 38,
      height: 4,
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.35),
        borderRadius: BorderRadius.circular(2),
      ),
    );
  }
}

class _QueueRow extends StatelessWidget {
  final Song song;
  final int listIndex;
  final bool isCurrent;
  final VoidCallback onTap;
  final VoidCallback? onRemove;

  const _QueueRow({
    super.key,
    required this.song,
    required this.listIndex,
    required this.isCurrent,
    required this.onTap,
    required this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    final fg = Colors.white;
    final highlightFill = Colors.white.withOpacity(0.12);
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 220),
          margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
          decoration: BoxDecoration(
            color: isCurrent ? highlightFill : Colors.transparent,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: SizedBox(
                  width: 44,
                  height: 44,
                  child: song.thumbnail.isEmpty
                      ? Container(
                          color: Colors.white.withOpacity(0.10),
                          child: Icon(Icons.music_note,
                              color: fg.withOpacity(0.6)),
                        )
                      : CachedNetworkImage(
                          imageUrl: song.thumbnail,
                          fit: BoxFit.cover,
                        ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      song.title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: fg,
                        fontWeight:
                            isCurrent ? FontWeight.w700 : FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      song.artist,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: fg.withOpacity(0.65),
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
              if (isCurrent)
                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 8),
                  child: Icon(Icons.graphic_eq, color: Colors.white, size: 20),
                ),
              if (onRemove != null)
                IconButton(
                  visualDensity: VisualDensity.compact,
                  icon: Icon(
                    Icons.remove_circle_outline,
                    color: fg.withOpacity(0.5),
                  ),
                  onPressed: onRemove,
                ),
              ReorderableDragStartListener(
                index: listIndex,
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: Icon(
                    Icons.drag_indicator,
                    color: fg.withOpacity(0.45),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
