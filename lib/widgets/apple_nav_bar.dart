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

/// Apple-Music-iOS-26-style floating dock:
///
///   [  Home  New  Radio  Library  ]   [ search ]
///
/// - Left: rounded "capsule" pill with the first N-1 tabs. The active tab
///   is highlighted by a sliding red filled pill that smoothly animates
///   between tabs.
/// - Right: a separate rounded circle for the last tab (search).
/// - Both surfaces use a real `BackdropFilter.blur(20, 20)` with a thin
///   translucent fill to look like liquid glass.
/// - Horizontal drag on the dock pages the underlying PageView.
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
    assert(items.length >= 2);
    final mainItems = items.take(items.length - 1).toList();
    final lastItem = items.last;
    final lastIndex = items.length - 1;
    final isLastActive = currentIndex == lastIndex;

    return SafeArea(
      top: false,
      minimum: const EdgeInsets.fromLTRB(12, 0, 12, 12),
      child: GestureDetector(
        behavior: HitTestBehavior.translucent,
        onHorizontalDragUpdate: (d) {
          if (onDrag != null) onDrag!(d.primaryDelta ?? 0);
        },
        child: Row(
          children: [
            Expanded(
              child: _CapsulePill(
                items: mainItems,
                activeIndex:
                    currentIndex < mainItems.length ? currentIndex : -1,
                onTap: (i) {
                  HapticFeedback.selectionClick();
                  onTap(i);
                },
              ),
            ),
            const SizedBox(width: 10),
            _CircleButton(
              item: lastItem,
              active: isLastActive,
              onTap: () {
                HapticFeedback.selectionClick();
                onTap(lastIndex);
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _CapsulePill extends StatelessWidget {
  final List<AppleNavItem> items;
  final int activeIndex;
  final ValueChanged<int> onTap;

  const _CapsulePill({
    required this.items,
    required this.activeIndex,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final glassFill = isDark
        ? Colors.white.withOpacity(0.08)
        : Colors.white.withOpacity(0.55);
    final glassBorder = isDark
        ? Colors.white.withOpacity(0.16)
        : Colors.black.withOpacity(0.08);

    return ClipRRect(
      borderRadius: BorderRadius.circular(36),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
        child: Container(
          height: 64,
          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
          decoration: BoxDecoration(
            color: glassFill,
            borderRadius: BorderRadius.circular(36),
            border: Border.all(color: glassBorder, width: 1),
          ),
          child: LayoutBuilder(
            builder: (context, constraints) {
              final tabWidth = constraints.maxWidth / items.length;
              return Stack(
                children: [
                  if (activeIndex >= 0 && activeIndex < items.length)
                    AnimatedPositioned(
                      duration: const Duration(milliseconds: 320),
                      curve: Curves.easeOutCubic,
                      left: tabWidth * activeIndex,
                      top: 0,
                      bottom: 0,
                      width: tabWidth,
                      child: Container(
                        decoration: BoxDecoration(
                          color: AppColors.accent,
                          borderRadius: BorderRadius.circular(28),
                        ),
                      ),
                    ),
                  Row(
                    children: List.generate(items.length, (i) {
                      final item = items[i];
                      final active = i == activeIndex;
                      return Expanded(
                        child: _PillTab(
                          item: item,
                          active: active,
                          onTap: () => onTap(i),
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
    );
  }
}

class _PillTab extends StatelessWidget {
  final AppleNavItem item;
  final bool active;
  final VoidCallback onTap;

  const _PillTab({
    required this.item,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final inactive = theme.colorScheme.onSurface.withOpacity(0.7);
    final color = active ? Colors.white : inactive;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            AnimatedScale(
              scale: active ? 1.05 : 1.0,
              duration: const Duration(milliseconds: 280),
              curve: Curves.easeOutCubic,
              child: Icon(
                active ? (item.activeIcon ?? item.icon) : item.icon,
                color: color,
                size: 22,
              ),
            ),
            const SizedBox(height: 2),
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

class _CircleButton extends StatelessWidget {
  final AppleNavItem item;
  final bool active;
  final VoidCallback onTap;

  const _CircleButton({
    required this.item,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final glassFill = isDark
        ? Colors.white.withOpacity(0.08)
        : Colors.white.withOpacity(0.55);
    final glassBorder = isDark
        ? Colors.white.withOpacity(0.16)
        : Colors.black.withOpacity(0.08);
    final iconColor = active
        ? Colors.white
        : theme.colorScheme.onSurface.withOpacity(0.85);

    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: ClipOval(
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
          child: Container(
            width: 64,
            height: 64,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: active ? AppColors.accent : glassFill,
              border: Border.all(color: glassBorder, width: 1),
            ),
            child: Icon(
              active ? (item.activeIcon ?? item.icon) : item.icon,
              color: iconColor,
              size: 24,
            ),
          ),
        ),
      ),
    );
  }
}
