import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

import '../models/song.dart';
import '../utils/theme.dart';

/// Standard list-row representation of a song.
class SongTile extends StatelessWidget {
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
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
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
                      style: const TextStyle(
                        color: AppColors.textPrimary,
                        fontWeight: FontWeight.w600,
                        fontSize: 15,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 3),
                    Text(
                      song.artist,
                      style: const TextStyle(
                        color: AppColors.textSecondary,
                        fontSize: 13,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              if (song.duration != null)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: Text(
                    song.durationLabel,
                    style: const TextStyle(
                      color: AppColors.textSecondary,
                      fontSize: 12,
                    ),
                  ),
                ),
              if (onAdd != null)
                IconButton(
                  icon: const Icon(Icons.add, color: Colors.white70, size: 22),
                  onPressed: onAdd,
                  tooltip: 'Add to playlist',
                ),
              IconButton(
                icon:
                    const Icon(Icons.more_vert, color: Colors.white70, size: 22),
                onPressed: onMenu,
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
    if (url.isEmpty) {
      return Container(
        width: 50,
        height: 50,
        color: AppColors.card,
        child: const Icon(Icons.music_note, color: Colors.white54),
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
        color: AppColors.card,
      ),
      errorWidget: (_, __, ___) => Container(
        width: 50,
        height: 50,
        color: AppColors.card,
        child: const Icon(Icons.broken_image, color: Colors.white54),
      ),
    );
  }
}
