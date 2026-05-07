import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../utils/theme.dart';

class AppleNavItem {
  final IconData icon;
  final IconData? activeIcon;
  final String label;

  const AppleNavItem({
    required this.icon,
    this.activeIcon,
    required this.label,
  });
}

/// Apple-Music-style full-width liquid-glass nav bar:
/// - Real `BackdropFilter.blur(20, 20)`
/// - Sits below an optional mini-player band
/// - Renders a sliding active indicator (300 ms spring) under the active tab
/// - Active tab tints in red, inactive tabs in `onSurface * 0.6`
/// - Thin separator on top to delineate the glass surface
/// - Swipe / drag horizontally to switch tabs (via `onDrag`)
class AppleNavBar extends StatelessWidget {
  final List<AppleNavItem> items;
  final int currentIndex;
  final ValueChanged<int> onTap;
  final ValueChanged<double>? onDrag;

  const AppleNavBar({
    super.key,
    required this.items,
    required this.currentIndex,
    required this.onTap,
    this.onDrag,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final glassFill = isDark
        ? Colors.white.withOpacity(0.06)
        : Colors.white.withOpacity(0.55);
    final separator = isDark
        ? Colors.white.withOpacity(0.12)
        : Colors.black.withOpacity(0.08);

    return BackdropFilter(
      filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
      child: Container(
        decoration: BoxDecoration(
          color: glassFill,
          border: Border(
            top: BorderSide(color: separator, width: 0.5),
          ),
        ),
        child: SafeArea(
          top: false,
          minimum: const EdgeInsets.only(bottom: 6),
          child: GestureDetector(
            behavior: HitTestBehavior.translucent,
            onHorizontalDragUpdate: (d) {
              if (onDrag != null) onDrag!(d.primaryDelta ?? 0);
            },
            child: SizedBox(
              height: 64,
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final tabWidth = constraints.maxWidth / items.length;
                  return Stack(
                    children: [
                      AnimatedPositioned(
                        duration: const Duration(milliseconds: 300),
                        curve: Curves.easeOutCubic,
                        left: tabWidth * currentIndex + tabWidth * 0.5 - 18,
                        top: 6,
                        child: Container(
                          width: 36,
                          height: 4,
                          decoration: BoxDecoration(
                            color: AppColors.accent,
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                      ),
                      Row(
                        children: List.generate(items.length, (i) {
                          final item = items[i];
                          final active = i == currentIndex;
                          return Expanded(
                            child: _NavTab(
                              item: item,
                              active: active,
                              onTap: () {
                                HapticFeedback.selectionClick();
                                onTap(i);
                              },
                            ),
                          );
                        }),
                      ),
                    ],
                  );
                },
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _NavTab extends StatelessWidget {
  final AppleNavItem item;
  final bool active;
  final VoidCallback onTap;

  const _NavTab({
    required this.item,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = active
        ? AppColors.accent
        : theme.colorScheme.onSurface.withOpacity(0.6);
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.only(top: 14, bottom: 6),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            AnimatedScale(
              scale: active ? 1.12 : 1.0,
              duration: const Duration(milliseconds: 300),
              curve: Curves.easeOutBack,
              child: Icon(
                active ? (item.activeIcon ?? item.icon) : item.icon,
                color: color,
                size: 24,
              ),
            ),
            const SizedBox(height: 3),
            AnimatedDefaultTextStyle(
              duration: const Duration(milliseconds: 200),
              style: TextStyle(
                color: color,
                fontSize: 11,
                fontWeight: active ? FontWeight.w700 : FontWeight.w500,
              ),
              child: Text(item.label),
            ),
          ],
        ),
      ),
    );
  }
}
