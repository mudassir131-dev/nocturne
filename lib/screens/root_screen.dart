import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../widgets/apple_nav_bar.dart';
import '../widgets/mini_player.dart';
import 'home_screen.dart';
import 'library_screen.dart';
import 'new_screen.dart';
import 'radio_screen.dart';
import 'search_screen.dart';

/// Apple-Music-style top-level scaffold:
/// 5-tab nav (Home, New, Radio, Library, Search) backed by a PageView so
/// content slides in from left/right with spring physics. Mini player sits
/// just above the full-width liquid-glass nav bar.
class RootScreen extends ConsumerStatefulWidget {
  const RootScreen({super.key});

  @override
  ConsumerState<RootScreen> createState() => _RootScreenState();
}

class _RootScreenState extends ConsumerState<RootScreen> {
  int _index = 0;
  late final PageController _pageController = PageController();

  static const List<AppleNavItem> _items = [
    AppleNavItem(
      icon: Icons.home_outlined,
      activeIcon: Icons.home_filled,
      label: 'Home',
    ),
    AppleNavItem(
      icon: Icons.grid_view_outlined,
      activeIcon: Icons.grid_view_rounded,
      label: 'New',
    ),
    AppleNavItem(
      icon: Icons.radio_outlined,
      activeIcon: Icons.radio,
      label: 'Radio',
    ),
    AppleNavItem(
      icon: Icons.library_music_outlined,
      activeIcon: Icons.library_music,
      label: 'Library',
    ),
    AppleNavItem(
      icon: Icons.search_outlined,
      activeIcon: Icons.search,
      label: 'Search',
    ),
  ];

  static const List<Widget> _pages = [
    HomeScreen(),
    NewScreen(),
    RadioScreen(),
    LibraryScreen(),
    SearchScreen(),
  ];

  void _switchTo(int i) {
    if (i == _index) return;
    _pageController.animateToPage(
      i,
      duration: const Duration(milliseconds: 320),
      curve: Curves.easeOutCubic,
    );
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBody: true,
      body: PageView(
        controller: _pageController,
        physics: const _SpringPageScrollPhysics(),
        onPageChanged: (i) => setState(() => _index = i),
        children: _pages,
      ),
      bottomNavigationBar: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Padding(
            padding: EdgeInsets.only(bottom: 6),
            child: MiniPlayer(),
          ),
          AppleNavBar(
            items: _items,
            currentIndex: _index,
            onTap: _switchTo,
          ),
        ],
      ),
    );
  }
}

/// Bouncy spring-style PageView physics so swiping between tabs feels closer
/// to iOS than the default clamping physics.
class _SpringPageScrollPhysics extends PageScrollPhysics {
  const _SpringPageScrollPhysics({super.parent});

  @override
  _SpringPageScrollPhysics applyTo(ScrollPhysics? ancestor) {
    return _SpringPageScrollPhysics(parent: buildParent(ancestor));
  }

  @override
  SpringDescription get spring => const SpringDescription(
        mass: 60,
        stiffness: 120,
        damping: 1.2,
      );
}
