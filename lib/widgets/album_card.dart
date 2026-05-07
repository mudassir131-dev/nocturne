import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

import '../utils/theme.dart';

/// 140x140 horizontal-scroll card used on the home screen and elsewhere.
/// Theme-aware text colours.
class AlbumCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final String imageUrl;
  final VoidCallback? onTap;
  final double size;

  const AlbumCard({
    super.key,
    required this.title,
    required this.subtitle,
    required this.imageUrl,
    this.onTap,
    this.size = 140,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final placeholder = theme.cardColor;
    final placeholderIcon = fg.withOpacity(0.45);
    return GestureDetector(
      onTap: onTap,
      child: SizedBox(
        width: size,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(AppRadius.card),
              child: SizedBox(
                width: size,
                height: size,
                child: imageUrl.isEmpty
                    ? Container(
                        color: placeholder,
                        child: Icon(
                          Icons.album,
                          color: placeholderIcon,
                          size: 48,
                        ),
                      )
                    : CachedNetworkImage(
                        imageUrl: imageUrl,
                        fit: BoxFit.cover,
                        memCacheWidth: 480,
                        memCacheHeight: 480,
                        placeholder: (_, __) => Container(
                          color: placeholder,
                        ),
                        errorWidget: (_, __, ___) => Container(
                          color: placeholder,
                          child: Icon(
                            Icons.broken_image,
                            color: placeholderIcon,
                          ),
                        ),
                      ),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                color: fg,
                fontWeight: FontWeight.w600,
                fontSize: 14,
              ),
            ),
            if (subtitle.isNotEmpty)
              Text(
                subtitle,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: theme.hintColor,
                  fontSize: 12,
                ),
              ),
          ],
        ),
      ),
    );
  }
}
