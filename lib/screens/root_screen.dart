import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../widgets/liquid_glass_dock.dart';
import '../widgets/mini_player.dart';
import 'home_screen.dart';
import 'library_screen.dart';
import 'new_screen.dart';
import 'radio_screen.dart';
import 'search_screen.dart';

/// Top-level scaffold with the liquid-glass bottom dock and the mini
/// player floating above it.
///
/// Tabs: Home / New / Radio / Library / Search.
///
/// The body is a [PageView] (springy iOS-feeling physics) so users can
/// swipe horizontally between tabs anywhere on screen. The dock itself
/// is also pannable: dragging across the dock moves the screen + the
/// red indicator with the finger in real time, then snaps to the
/// nearest tab on release.
class RootScreen extends ConsumerStatefulWidget {
  const RootScreen({super.key});

  @override
  ConsumerState<RootScreen> createState() => _RootScreenState();
}

class _RootScreenState extends ConsumerState<RootScreen> {
  late final PageController _controller;
  int _index = 0;

  static const List<DockItem> _items = [
    DockItem(icon: CupertinoIcons.house_fill, label: 'Home'),
    DockItem(icon: CupertinoIcons.sparkles, label: 'New'),
    DockItem(icon: CupertinoIcons.dot_radiowaves_left_right, label: 'Radio'),
    DockItem(icon: CupertinoIcons.square_stack_3d_up, label: 'Library'),
    DockItem(icon: CupertinoIcons.search, label: 'Search'),
  ];

  static const List<Widget> _pages = [
    HomeScreen(),
    NewScreen(),
    RadioScreen(),
    LibraryScreen(),
    SearchScreen(),
  ];

  @override
  void initState() {
    super.initState();
    _controller = PageController(initialPage: 0);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _selectTab(int i) {
    if (i == _index) return;
    _controller.animateToPage(
      i,
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeOutCubic,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBody: true,
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: PageView(
        controller: _controller,
        physics: const _IosTabsPhysics(),
        onPageChanged: (i) => setState(() => _index = i),
        children: _pages,
      ),
      bottomNavigationBar: SafeArea(
        top: false,
        bottom: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Padding(
              padding: EdgeInsets.only(bottom: 4),
              child: MiniPlayer(),
            ),
            LiquidGlassDock(
              items: _items,
              controller: _controller,
              currentIndex: _index,
              onTap: _selectTab,
            ),
          ],
        ),
      ),
    );
  }
}

/// Slightly springier scroll feel for tab swiping.
class _IosTabsPhysics extends PageScrollPhysics {
  const _IosTabsPhysics();

  @override
  SpringDescription get spring => const SpringDescription(
        mass: 0.5,
        stiffness: 110,
        damping: 14,
      );

  @override
  PageScrollPhysics applyTo(ScrollPhysics? ancestor) => const _IosTabsPhysics();
}
