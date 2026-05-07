import 'dart:ui';

import 'package:flutter/material.dart';

import '../utils/theme.dart';

/// Item describing a single tab in [LiquidGlassDock].
class DockItem {
  final IconData icon;
  final String label;
  const DockItem({required this.icon, required this.label});
}

/// Floating, glassmorphic bottom navigation dock. Theme-aware so it
/// reads correctly on both light and dark backgrounds.
class LiquidGlassDock extends StatelessWidget {
  final List<DockItem> items;
  final int currentIndex;
  final ValueChanged<int> onTap;

  const LiquidGlassDock({
    super.key,
    required this.items,
    required this.currentIndex,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final glassFill = isDark
        ? Colors.white.withOpacity(0.13)
        : Colors.black.withOpacity(0.04);
    final glassBorder = isDark
        ? Colors.white.withOpacity(0.28)
        : Colors.black.withOpacity(0.10);
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 0, 20, 16),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(AppRadius.dock),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 25, sigmaY: 25),
          child: Container(
            height: 68,
            decoration: BoxDecoration(
              color: glassFill,
              borderRadius: BorderRadius.circular(AppRadius.dock),
              border: Border.all(color: glassBorder, width: 1.5),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: List.generate(items.length, (i) {
                final selected = i == currentIndex;
                return _DockButton(
                  item: items[i],
                  selected: selected,
                  onTap: () => onTap(i),
                );
              }),
            ),
          ),
        ),
      ),
    );
  }
}

class _DockButton extends StatelessWidget {
  final DockItem item;
  final bool selected;
  final VoidCallback onTap;

  const _DockButton({
    required this.item,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final accent = theme.colorScheme.primary;
    return AnimatedContainer(
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOutCubic,
      decoration: BoxDecoration(
        color: selected ? accent.withOpacity(0.18) : Colors.transparent,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(20),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
            child: AnimatedScale(
              duration: const Duration(milliseconds: 220),
              scale: selected ? 1.08 : 1.0,
              curve: Curves.easeOutCubic,
              child: Icon(
                item.icon,
                size: 26,
                color: selected ? accent : fg.withOpacity(0.6),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
