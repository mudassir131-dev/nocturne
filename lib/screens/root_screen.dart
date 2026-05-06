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
/// floating above it.
class RootScreen extends ConsumerStatefulWidget {
  const RootScreen({super.key});

  @override
  ConsumerState<RootScreen> createState() => _RootScreenState();
}

class _RootScreenState extends ConsumerState<RootScreen> {
  int _index = 0;

  static const List<DockItem> _items = [
    DockItem(icon: Icons.home_filled, label: 'Home'),
    DockItem(icon: Icons.search, label: 'Search'),
    DockItem(icon: Icons.library_music, label: 'Library'),
    DockItem(icon: Icons.favorite, label: 'Liked'),
    DockItem(icon: Icons.person, label: 'Profile'),
  ];

  Widget _pageFor(int index) {
    switch (index) {
      case 0:
        return const HomeScreen();
      case 1:
        return const SearchScreen();
      case 2:
        return const LibraryScreen();
      case 3:
        return const LikedScreen();
      case 4:
      default:
        return const ProfileScreen();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBody: true,
      body: AnimatedSwitcher(
        duration: const Duration(milliseconds: 220),
        switchInCurve: Curves.easeOutCubic,
        switchOutCurve: Curves.easeInCubic,
        child: KeyedSubtree(
          key: ValueKey<int>(_index),
          child: _pageFor(_index),
        ),
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
              onTap: (i) => setState(() => _index = i),
            ),
          ],
        ),
      ),
    );
  }
}
