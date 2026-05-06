import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nocturne/utils/theme.dart';

void main() {
  testWidgets('App theme exposes the Nocturne palette', (tester) async {
    final theme = AppTheme.dark();
    expect(theme.scaffoldBackgroundColor, AppColors.background);
    expect(theme.colorScheme.primary, AppColors.accent);
  });

  testWidgets('Renders a placeholder MaterialApp without error',
      (tester) async {
    await tester.pumpWidget(MaterialApp(
      theme: AppTheme.dark(),
      home: const Scaffold(
        body: Center(child: Text('Nocturne')),
      ),
    ));
    expect(find.text('Nocturne'), findsOneWidget);
  });
}
