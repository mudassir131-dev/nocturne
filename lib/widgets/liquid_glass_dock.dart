import 'dart:ui';

import 'package:flutter/material.dart';

import '../utils/theme.dart';

/// Item describing a single tab in [LiquidGlassDock].
class DockItem {
  final IconData icon;
  final String label;
  const DockItem({required this.icon, required this.label});
}

/// iOS-26-style floating dock:
///
///   [ Home  New  Radio  Library ]   ( 🔍 )
///   ───────── pill ─────────────    circle
///
/// The pill is a frosted-glass capsule with N tabs and a red liquid pill
/// indicator that slides between them. The trailing circle is a separate
/// glass button that opens the search screen (handled by the host).
///
/// Pill behaviour:
/// - Tapping a tab animates the body's [PageController] to that page.
/// - Dragging horizontally on the pill drags the screen + indicator with
///   the finger 1:1, then snaps to the nearest tab on release with spring
///   physics (~300 ms). The indicator also tracks the active page when
///   the user swipes the body PageView.
class LiquidGlassDock extends StatefulWidget {
  final List<DockItem> items;
  final PageController controller;
  final ValueChanged<int> onTap;
  final int currentIndex;
  final VoidCallback? onSearchTap;

  const LiquidGlassDock({
    super.key,
    required this.items,
    required this.controller,
    required this.onTap,
    required this.currentIndex,
    this.onSearchTap,
  });

  static const double _height = 64;

  @override
  State<LiquidGlassDock> createState() => _LiquidGlassDockState();
}

class _LiquidGlassDockState extends State<LiquidGlassDock> {
  double _dragPage = 0;
  bool _dragging = false;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final glassFill = isDark
        ? Colors.white.withOpacity(0.10)
        : Colors.black.withOpacity(0.04);
    final glassBorder = (isDark ? Colors.white : Colors.black).withOpacity(0.18);

    return Padding(
      padding: EdgeInsets.fromLTRB(
        16,
        4,
        16,
        MediaQuery.of(context).padding.bottom > 0 ? 4 : 12,
      ),
      child: Row(
        children: [
          Expanded(
            child: _GlassPill(
              height: LiquidGlassDock._height,
              fill: glassFill,
              border: glassBorder,
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final cellWidth =
                      constraints.maxWidth / widget.items.length;
                  return GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onHorizontalDragStart: (_) => _onDragStart(),
                    onHorizontalDragUpdate: (d) =>
                        _onDragUpdate(d.localPosition, cellWidth),
                    onHorizontalDragEnd: (_) => _onDragEnd(),
                    onHorizontalDragCancel: _onDragEnd,
                    child: Stack(
                      children: [
                        AnimatedBuilder(
                          animation: widget.controller,
                          builder: (context, _) {
                            final page = _resolvedPage();
                            return Positioned(
                              left: cellWidth * page + cellWidth * 0.10,
                              top: 6,
                              bottom: 6,
                              width: cellWidth * 0.80,
                              child: const _LiquidIndicator(),
                            );
                          },
                        ),
                        Row(
                          children: List.generate(widget.items.length, (i) {
                            return Expanded(
                              child: AnimatedBuilder(
                                animation: widget.controller,
                                builder: (context, _) {
                                  final page = _resolvedPage();
                                  final t =
                                      (1 - (page - i).abs()).clamp(0.0, 1.0);
                                  return _DockButton(
                                    item: widget.items[i],
                                    activeFraction: t,
                                    onTap: () => widget.onTap(i),
                                  );
                                },
                              ),
                            );
                          }),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
          ),
          if (widget.onSearchTap != null) ...[
            const SizedBox(width: 10),
            _GlassCircleButton(
              size: LiquidGlassDock._height,
              fill: glassFill,
              border: glassBorder,
              icon: Icons.search,
              onTap: widget.onSearchTap!,
            ),
          ],
        ],
      ),
    );
  }

  double _resolvedPage() {
    if (_dragging) return _dragPage;
    if (widget.controller.hasClients && widget.controller.page != null) {
      return widget.controller.page!;
    }
    return widget.currentIndex.toDouble();
  }

  void _onDragStart() {
    if (!widget.controller.hasClients) return;
    setState(() {
      _dragging = true;
      _dragPage = widget.controller.page ?? widget.currentIndex.toDouble();
    });
  }

  void _onDragUpdate(Offset local, double cellWidth) {
    if (!widget.controller.hasClients) return;
    if (cellWidth <= 0) return;
    final viewportWidth = widget.controller.position.viewportDimension;
    if (viewportWidth <= 0) return;
    final fingerPage = (local.dx / cellWidth).clamp(
      0.0,
      (widget.items.length - 1).toDouble(),
    );
    setState(() => _dragPage = fingerPage);
    widget.controller.jumpTo(fingerPage * viewportWidth);
  }

  void _onDragEnd() {
    if (!widget.controller.hasClients) {
      if (mounted) setState(() => _dragging = false);
      return;
    }
    final target = _dragPage.round().clamp(0, widget.items.length - 1);
    widget.controller
        .animateToPage(
          target,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOutCubic,
        )
        .whenComplete(() {
      if (mounted) setState(() => _dragging = false);
    });
  }
}

/// Frosted glass capsule used for both the tab pill and the circle button.
class _GlassPill extends StatelessWidget {
  final double height;
  final Color fill;
  final Color border;
  final Widget child;

  const _GlassPill({
    required this.height,
    required this.fill,
    required this.border,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    final radius = height / 2;
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 28, sigmaY: 28),
        child: Container(
          height: height,
          decoration: BoxDecoration(
            color: fill,
            borderRadius: BorderRadius.circular(radius),
            border: Border.all(color: border, width: 1),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.18),
                blurRadius: 24,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: child,
        ),
      ),
    );
  }
}

class _GlassCircleButton extends StatelessWidget {
  final double size;
  final Color fill;
  final Color border;
  final IconData icon;
  final VoidCallback onTap;

  const _GlassCircleButton({
    required this.size,
    required this.fill,
    required this.border,
    required this.icon,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return SizedBox(
      width: size,
      height: size,
      child: ClipOval(
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 28, sigmaY: 28),
          child: Material(
            color: fill,
            shape: CircleBorder(
              side: BorderSide(color: border, width: 1),
            ),
            child: InkWell(
              customBorder: const CircleBorder(),
              onTap: onTap,
              child: Center(
                child: Icon(
                  icon,
                  size: 22,
                  color: theme.colorScheme.onSurface,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _LiquidIndicator extends StatelessWidget {
  const _LiquidIndicator();

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
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
            borderRadius: BorderRadius.circular(20),
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
  final double activeFraction;
  final VoidCallback onTap;

  const _DockButton({
    required this.item,
    required this.activeFraction,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final color = Color.lerp(
      fg.withOpacity(0.6),
      Colors.white,
      activeFraction,
    )!;
    final accent = Color.lerp(color, AppColors.accent, activeFraction)!;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(20),
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(item.icon, size: 22, color: accent),
              const SizedBox(height: 2),
              Text(
                item.label,
                style: TextStyle(
                  color: accent,
                  fontSize: 10,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.2,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
