import 'dart:ui';

import 'package:flutter/material.dart';

import '../utils/theme.dart';

/// Glassmorphism search field used at the top of the search screen.
/// Theme-aware: text + glass colours flip between light and dark.
class LiquidGlassSearchBar extends StatelessWidget {
  final TextEditingController controller;
  final ValueChanged<String>? onChanged;
  final VoidCallback? onSubmitted;
  final String hint;

  const LiquidGlassSearchBar({
    super.key,
    required this.controller,
    this.onChanged,
    this.onSubmitted,
    this.hint = 'Search songs, artists, albums...',
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final fg = theme.colorScheme.onSurface;
    final glassFill = isDark
        ? Colors.white.withOpacity(0.13)
        : Colors.black.withOpacity(0.04);
    final glassBorder = isDark
        ? Colors.white.withOpacity(0.28)
        : Colors.black.withOpacity(0.10);

    return ClipRRect(
      borderRadius: BorderRadius.circular(AppRadius.searchBar),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 25, sigmaY: 25),
        child: Container(
          decoration: BoxDecoration(
            color: glassFill,
            borderRadius: BorderRadius.circular(AppRadius.searchBar),
            border: Border.all(color: glassBorder, width: 1.5),
          ),
          child: TextField(
            controller: controller,
            onChanged: onChanged,
            onSubmitted: (_) => onSubmitted?.call(),
            cursorColor: AppColors.accent,
            style: TextStyle(color: fg, fontSize: 16),
            decoration: InputDecoration(
              border: InputBorder.none,
              hintText: hint,
              hintStyle: TextStyle(color: fg.withOpacity(0.55)),
              prefixIcon: Icon(Icons.search, color: fg.withOpacity(0.7)),
              suffixIcon: ValueListenableBuilder<TextEditingValue>(
                valueListenable: controller,
                builder: (_, value, __) => value.text.isEmpty
                    ? const SizedBox.shrink()
                    : IconButton(
                        icon: Icon(Icons.close, color: fg.withOpacity(0.7)),
                        onPressed: () {
                          controller.clear();
                          onChanged?.call('');
                        },
                      ),
              ),
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 8, vertical: 16),
            ),
          ),
        ),
      ),
    );
  }
}
