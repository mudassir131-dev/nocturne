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
///
/// The dock is **driven by the host's [PageController]**: as the user
/// swipes between tabs (either via this dock or via the body's own
/// PageView) the controller's `page` value drives the indicator's
/// position in real time. The dock additionally exposes its own
/// horizontal-pan handler so users can drag a finger across the dock
/// and see the screen + indicator track the finger 1:1, then snap to
/// the nearest tab on release with spring physics (~300ms).
class LiquidGlassDock extends StatefulWidget {
  final List<DockItem> items;
  final PageController controller;
  final ValueChanged<int> onTap;
  final int currentIndex;

  const LiquidGlassDock({
    super.key,
    required this.items,
    required this.controller,
    required this.onTap,
    required this.currentIndex,
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
    final topBorder = (isDark ? Colors.white : Colors.black).withOpacity(0.15);

    return Container(
      decoration: BoxDecoration(
        border: Border(top: BorderSide(color: topBorder, width: 0.5)),
      ),
      child: ClipRect(
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 25, sigmaY: 25),
          child: Container(
            color: glassFill,
            padding: EdgeInsets.only(
              top: 6,
              bottom: MediaQuery.of(context).padding.bottom > 0 ? 0 : 6,
            ),
            child: SizedBox(
              height: LiquidGlassDock._height,
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final cellWidth = constraints.maxWidth / widget.items.length;
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
                              left: cellWidth * page + cellWidth * 0.18,
                              top: 6,
                              bottom: 6,
                              width: cellWidth * 0.64,
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
        ),
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
    // Drive the body PageView so the screen tracks the finger.
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
