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

/// Top-level scaffold with the iOS-26-style floating dock + mini player.
///
/// Layout matches the Apple Music dock:
///
///   [ Home  New  Radio  Library ]   ( 🔍 )
///
/// The pill is a horizontal PageView (Home / New / Radio / Library); the
/// circle on the right opens [SearchScreen] as a fullscreen route push so
/// the dock and main pages stay in their pill while search has its own
/// hero-friendly stack.
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
    DockItem(icon: CupertinoIcons.square_grid_2x2_fill, label: 'New'),
    DockItem(icon: CupertinoIcons.dot_radiowaves_left_right, label: 'Radio'),
    DockItem(icon: CupertinoIcons.music_albums_fill, label: 'Library'),
  ];

  static const List<Widget> _pages = [
    HomeScreen(),
    NewScreen(),
    RadioScreen(),
    LibraryScreen(),
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

  void _openSearch() {
    Navigator.of(context).push(
      PageRouteBuilder<void>(
        opaque: false,
        barrierColor: Colors.black54,
        transitionDuration: const Duration(milliseconds: 280),
        pageBuilder: (_, __, ___) => const SearchScreen(),
        transitionsBuilder: (_, animation, __, child) {
          final curved = CurvedAnimation(
            parent: animation,
            curve: Curves.easeOutCubic,
          );
          return SlideTransition(
            position: Tween<Offset>(
              begin: const Offset(0, 0.06),
              end: Offset.zero,
            ).animate(curved),
            child: FadeTransition(opacity: curved, child: child),
          );
        },
      ),
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
              onSearchTap: _openSearch,
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
