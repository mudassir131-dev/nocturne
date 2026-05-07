import 'dart:ui';

import 'package:flutter/material.dart';

import '../utils/theme.dart';

/// Item describing a single tab in [LiquidGlassDock].
class DockItem {
  final IconData icon;
  final String label;
  const DockItem({required this.icon, required this.label});
}

/// Floating glass dock with a "liquid" red indicator that slides smoothly
/// to the selected tab. The indicator rides above the icons (frosted-glass
/// pill) and is what gives the bar its iOS 26 feel.
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

  static const double _height = 68;

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
          filter: ImageFilter.blur(sigmaX: 30, sigmaY: 30),
          child: LayoutBuilder(builder: (context, constraints) {
            final cellWidth = constraints.maxWidth / items.length;
            return SizedBox(
              height: _height,
              child: Stack(
                children: [
                  Container(
                    decoration: BoxDecoration(
                      color: glassFill,
                      borderRadius: BorderRadius.circular(AppRadius.dock),
                      border: Border.all(color: glassBorder, width: 1.5),
                    ),
                  ),
                  AnimatedPositioned(
                    duration: const Duration(milliseconds: 320),
                    curve: Curves.easeOutCubic,
                    left: cellWidth * currentIndex + cellWidth * 0.16,
                    top: 8,
                    bottom: 8,
                    width: cellWidth * 0.68,
                    child: _LiquidIndicator(),
                  ),
                  Row(
                    children: List.generate(items.length, (i) {
                      final selected = i == currentIndex;
                      return Expanded(
                        child: _DockButton(
                          item: items[i],
                          selected: selected,
                          onTap: () => onTap(i),
                        ),
                      );
                    }),
                  ),
                ],
              ),
            );
          }),
        ),
      ),
    );
  }
}

class _LiquidIndicator extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(22),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
        child: Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [
                AppColors.accent.withOpacity(0.95),
                AppColors.accent.withOpacity(0.65),
              ],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(22),
            border: Border.all(
              color: Colors.white.withOpacity(0.25),
              width: 1.0,
            ),
            boxShadow: [
              BoxShadow(
                color: AppColors.accent.withOpacity(0.45),
                blurRadius: 16,
                spreadRadius: 0,
              ),
            ],
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
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(22),
        child: Center(
          child: AnimatedScale(
            scale: selected ? 1.10 : 1.0,
            duration: const Duration(milliseconds: 220),
            curve: Curves.easeOutCubic,
            child: Icon(
              item.icon,
              size: 26,
              color: selected ? Colors.white : fg.withOpacity(0.6),
            ),
          ),
        ),
      ),
    );
  }
}
