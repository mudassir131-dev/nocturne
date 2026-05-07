import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../widgets/liquid_glass_dock.dart';
import '../widgets/mini_player.dart';
import 'home_screen.dart';
import 'liked_screen.dart';
import 'library_screen.dart';
import 'profile_screen.dart';
import 'search_screen.dart';

/// Top-level scaffold with the liquid-glass bottom dock and the mini player
/// floating above it. The body is a [PageView] so users can swipe
/// horizontally between tabs (with the red indicator sliding in sync).
class RootScreen extends ConsumerStatefulWidget {
  const RootScreen({super.key});

  @override
  ConsumerState<RootScreen> createState() => _RootScreenState();
}

class _RootScreenState extends ConsumerState<RootScreen> {
  late final PageController _controller;
  int _index = 0;

  static const List<DockItem> _items = [
    DockItem(icon: Icons.home_filled, label: 'Home'),
    DockItem(icon: Icons.search, label: 'Search'),
    DockItem(icon: Icons.library_music, label: 'Library'),
    DockItem(icon: Icons.favorite, label: 'Liked'),
    DockItem(icon: Icons.person, label: 'Profile'),
  ];

  static const List<Widget> _pages = [
    HomeScreen(),
    SearchScreen(),
    LibraryScreen(),
    LikedScreen(),
    ProfileScreen(),
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
      duration: const Duration(milliseconds: 380),
      curve: Curves.easeOutCubic,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBody: true,
      body: PageView(
        controller: _controller,
        physics: const _IosTabsPhysics(),
        onPageChanged: (i) => setState(() => _index = i),
        children: _pages,
      ),
      bottomNavigationBar: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Padding(
              padding: EdgeInsets.only(bottom: 8),
              child: MiniPlayer(),
            ),
            LiquidGlassDock(
              items: _items,
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
