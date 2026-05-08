import 'dart:ui';

import 'package:flutter/material.dart';

/// Reusable iOS frosted-glass surface. Used by the player's three-dot menu,
/// the queue sheet, and the lyrics backdrop so they all share a consistent
/// blur intensity, fill and border.
class GlassPanel extends StatelessWidget {
  final Widget child;
  final BorderRadius? radius;
  final double sigma;
  final double? fillOpacity;
  final EdgeInsetsGeometry padding;

  const GlassPanel({
    super.key,
    required this.child,
    this.radius,
    this.sigma = 28,
    this.fillOpacity,
    this.padding = const EdgeInsets.all(16),
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final fill = (isDark ? Colors.white : Colors.black)
        .withOpacity(fillOpacity ?? (isDark ? 0.10 : 0.04));
    final border = (isDark ? Colors.white : Colors.black)
        .withOpacity(isDark ? 0.18 : 0.08);
    return ClipRRect(
      borderRadius: radius ?? BorderRadius.circular(22),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: sigma, sigmaY: sigma),
        child: Container(
          padding: padding,
          decoration: BoxDecoration(
            color: fill,
            borderRadius: radius ?? BorderRadius.circular(22),
            border: Border.all(color: border, width: 1.2),
          ),
          child: child,
        ),
      ),
    );
  }
}
