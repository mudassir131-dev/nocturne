import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../services/download_service.dart';
import 'song_actions_sheet.dart';

/// Standard list-row representation of a song. Long-press opens the
/// shared actions sheet (download / lyrics / share / play next).
class SongTile extends ConsumerWidget {
  final Song song;
  final VoidCallback? onTap;
  final VoidCallback? onAdd;
  final VoidCallback? onMenu;
  final bool dense;

  const SongTile({
    super.key,
    required this.song,
    this.onTap,
    this.onAdd,
    this.onMenu,
    this.dense = false,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final secondary = theme.hintColor;
    final downloaded =
        ref.watch(downloadServiceProvider).isDownloaded(song.videoId);
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        onLongPress: () => showSongActions(context, ref, song),
        child: Padding(
          padding: EdgeInsets.symmetric(
            horizontal: 16,
            vertical: dense ? 6 : 10,
          ),
          child: Row(
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: _Thumb(url: song.thumbnail),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      song.title,
                      style: TextStyle(
                        color: fg,
                        fontWeight: FontWeight.w600,
                        fontSize: 15,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 3),
                    Text(
                      song.artist,
                      style: TextStyle(
                        color: secondary,
                        fontSize: 13,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              if (downloaded)
                Padding(
                  padding: const EdgeInsets.only(right: 6),
                  child: Icon(
                    Icons.download_done,
                    color: fg.withOpacity(0.7),
                    size: 18,
                  ),
                ),
              if (song.duration != null)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: Text(
                    song.durationLabel,
                    style: TextStyle(
                      color: secondary,
                      fontSize: 12,
                    ),
                  ),
                ),
              if (onAdd != null)
                IconButton(
                  icon: Icon(Icons.add, color: fg.withOpacity(0.7), size: 22),
                  onPressed: onAdd,
                  tooltip: 'Add to queue',
                ),
              IconButton(
                icon: Icon(Icons.more_vert, color: fg.withOpacity(0.7), size: 22),
                onPressed: onMenu ?? () => showSongActions(context, ref, song),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Thumb extends StatelessWidget {
  final String url;
  const _Thumb({required this.url});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final placeholderColor = theme.cardColor;
    final iconColor = theme.colorScheme.onSurface.withOpacity(0.45);
    if (url.isEmpty) {
      return Container(
        width: 50,
        height: 50,
        color: placeholderColor,
        child: Icon(Icons.music_note, color: iconColor),
      );
    }
    return CachedNetworkImage(
      imageUrl: url,
      width: 50,
      height: 50,
      fit: BoxFit.cover,
      placeholder: (_, __) => Container(
        width: 50,
        height: 50,
        color: placeholderColor,
      ),
      errorWidget: (_, __, ___) => Container(
        width: 50,
        height: 50,
        color: placeholderColor,
        child: Icon(Icons.broken_image, color: iconColor),
      ),
    );
  }
}
