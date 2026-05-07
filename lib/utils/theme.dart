import 'package:flutter/material.dart';

/// Centralized color palette for the Nocturne app.
class AppColors {
  AppColors._();

  // Brand
  static const Color accent = Color(0xFFE53935);

  // Dark theme
  static const Color background = Color(0xFF000000);
  static const Color card = Color(0xFF1A1A1A);
  static const Color surface = Color(0xFF111111);
  static const Color textPrimary = Colors.white;
  static const Color textSecondary = Color(0xFF888888);
  static const Color glassBorder = Color(0x40FFFFFF);

  // Light theme
  static const Color lightBackground = Color(0xFFFAFAFA);
  static const Color lightCard = Color(0xFFFFFFFF);
  static const Color lightSurface = Color(0xFFF2F2F2);
  static const Color lightTextPrimary = Color(0xFF111111);
  static const Color lightTextSecondary = Color(0xFF6B6B6B);
  static const Color lightGlassBorder = Color(0x33000000);
}

/// App-wide constants for spacing/radius so cards stay consistent.
class AppRadius {
  AppRadius._();

  static const double card = 16;
  static const double dock = 35;
  static const double searchBar = 25;
  static const double player = 24;
}

/// Brand metadata. Surfaced in the splash screen + about/profile screen.
class AppBranding {
  AppBranding._();

  static const String version = 'v0.3';
  static const String developer = 'developed by demonslxyer';
  static const String tagline = 'Pure music. Liquid feel.';
}

class AppTheme {
  AppTheme._();

  static ThemeData dark() {
    final base = ThemeData.dark(useMaterial3: true);
    final textTheme = base.textTheme.apply(
      fontFamily: 'SF Pro Display',
      fontFamilyFallback: const ['Inter', 'Roboto'],
      bodyColor: AppColors.textPrimary,
      displayColor: AppColors.textPrimary,
    );

    return base.copyWith(
      brightness: Brightness.dark,
      scaffoldBackgroundColor: AppColors.background,
      colorScheme: const ColorScheme.dark(
        primary: AppColors.accent,
        secondary: AppColors.accent,
        surface: AppColors.card,
        onPrimary: Colors.white,
        onSurface: AppColors.textPrimary,
      ),
      cardColor: AppColors.card,
      textTheme: textTheme,
      hintColor: AppColors.textSecondary,
      iconTheme: const IconThemeData(color: AppColors.textPrimary),
      appBarTheme: AppBarTheme(
        backgroundColor: AppColors.background,
        elevation: 0,
        centerTitle: false,
        iconTheme: const IconThemeData(color: AppColors.textPrimary),
        titleTextStyle: textTheme.titleLarge?.copyWith(
          fontWeight: FontWeight.w700,
        ),
      ),
      sliderTheme: const SliderThemeData(
        activeTrackColor: AppColors.accent,
        inactiveTrackColor: Color(0xFF333333),
        thumbColor: AppColors.accent,
        overlayColor: Color(0x33E53935),
        trackHeight: 3,
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: AppColors.accent,
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: AppColors.card,
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: AppColors.card,
        contentTextStyle: textTheme.bodyMedium,
        behavior: SnackBarBehavior.floating,
      ),
      pageTransitionsTheme: const PageTransitionsTheme(
        builders: {
          TargetPlatform.android: CupertinoPageTransitionsBuilder(),
          TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
        },
      ),
    );
  }

  static ThemeData light() {
    final base = ThemeData.light(useMaterial3: true);
    final textTheme = base.textTheme.apply(
      fontFamily: 'SF Pro Display',
      fontFamilyFallback: const ['Inter', 'Roboto'],
      bodyColor: AppColors.lightTextPrimary,
      displayColor: AppColors.lightTextPrimary,
    );

    return base.copyWith(
      brightness: Brightness.light,
      scaffoldBackgroundColor: AppColors.lightBackground,
      colorScheme: const ColorScheme.light(
        primary: AppColors.accent,
        secondary: AppColors.accent,
        surface: AppColors.lightCard,
        onPrimary: Colors.white,
        onSurface: AppColors.lightTextPrimary,
      ),
      cardColor: AppColors.lightCard,
      textTheme: textTheme,
      hintColor: AppColors.lightTextSecondary,
      iconTheme: const IconThemeData(color: AppColors.lightTextPrimary),
      appBarTheme: AppBarTheme(
        backgroundColor: AppColors.lightBackground,
        elevation: 0,
        centerTitle: false,
        iconTheme: const IconThemeData(color: AppColors.lightTextPrimary),
        titleTextStyle: textTheme.titleLarge?.copyWith(
          fontWeight: FontWeight.w700,
          color: AppColors.lightTextPrimary,
        ),
      ),
      sliderTheme: const SliderThemeData(
        activeTrackColor: AppColors.accent,
        inactiveTrackColor: Color(0xFFE0E0E0),
        thumbColor: AppColors.accent,
        overlayColor: Color(0x33E53935),
        trackHeight: 3,
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: AppColors.accent,
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: AppColors.lightCard,
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: AppColors.lightCard,
        contentTextStyle: textTheme.bodyMedium,
        behavior: SnackBarBehavior.floating,
      ),
      pageTransitionsTheme: const PageTransitionsTheme(
        builders: {
          TargetPlatform.android: CupertinoPageTransitionsBuilder(),
          TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
        },
      ),
    );
  }
}
