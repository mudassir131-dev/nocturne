import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Centralized color palette for the Nocturne app.
class AppColors {
  AppColors._();

  static const Color background = Color(0xFF000000);
  static const Color card = Color(0xFF1A1A1A);
  static const Color surface = Color(0xFF111111);
  static const Color accent = Color(0xFFE53935);
  static const Color textPrimary = Colors.white;
  static const Color textSecondary = Color(0xFF888888);
  static const Color glassBorder = Color(0x40FFFFFF);
}

/// App-wide constants for spacing/radius so cards stay consistent.
class AppRadius {
  AppRadius._();

  static const double card = 16;
  static const double dock = 35;
  static const double searchBar = 25;
  static const double player = 24;
}

class AppTheme {
  AppTheme._();

  static ThemeData dark() {
    final base = ThemeData.dark(useMaterial3: true);
    final textTheme = GoogleFonts.interTextTheme(base.textTheme).apply(
      bodyColor: AppColors.textPrimary,
      displayColor: AppColors.textPrimary,
    );

    return base.copyWith(
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
    );
  }
}
