import 'package:flutter/material.dart';
import 'package:shimmer/shimmer.dart';

import '../utils/theme.dart';

/// Reusable shimmer skeletons for loading states. Theme-aware.
class ShimmerBox extends StatelessWidget {
  final double height;
  final double? width;
  final BorderRadius? borderRadius;

  const ShimmerBox({
    super.key,
    required this.height,
    this.width,
    this.borderRadius,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    return Shimmer.fromColors(
      baseColor: isDark ? const Color(0xFF1A1A1A) : const Color(0xFFE6E6E6),
      highlightColor:
          isDark ? const Color(0xFF2C2C2C) : const Color(0xFFF6F6F6),
      child: Container(
        height: height,
        width: width ?? double.infinity,
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: borderRadius ?? BorderRadius.circular(8),
        ),
      ),
    );
  }
}

class ShimmerSongList extends StatelessWidget {
  final int count;
  final bool shrinkWrap;
  const ShimmerSongList({
    super.key,
    this.count = 6,
    this.shrinkWrap = true,
  });

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      shrinkWrap: shrinkWrap,
      physics: shrinkWrap
          ? const NeverScrollableScrollPhysics()
          : const ClampingScrollPhysics(),
      itemCount: count,
      itemBuilder: (_, __) => const Padding(
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 6),
        child: Row(
          children: [
            ShimmerBox(height: 50, width: 50),
            SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ShimmerBox(height: 12),
                  SizedBox(height: 6),
                  ShimmerBox(height: 10, width: 140),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class ShimmerHorizontalCards extends StatelessWidget {
  final int count;
  const ShimmerHorizontalCards({super.key, this.count = 4});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 200,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 20),
        itemCount: count,
        separatorBuilder: (_, __) => const SizedBox(width: 12),
        itemBuilder: (_, __) => const SizedBox(
          width: 140,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ShimmerBox(
                height: 140,
                borderRadius: BorderRadius.all(Radius.circular(AppRadius.card)),
              ),
              SizedBox(height: 8),
              ShimmerBox(height: 12),
              SizedBox(height: 6),
              ShimmerBox(height: 10, width: 80),
            ],
          ),
        ),
      ),
    );
  }
}
