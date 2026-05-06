import 'package:audio_service/audio_service.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';

import 'screens/root_screen.dart';
import 'services/audio_service.dart' as nocturne_audio;
import 'utils/theme.dart';

/// Entry point. Initializes Hive (offline cache), the AudioService background
/// audio handler, and (best-effort) Firebase, then runs the app.
///
/// Firebase initialization is intentionally tolerant of misconfiguration:
/// if `firebase_options.dart` hasn't been generated yet, or if
/// `Firebase.initializeApp` throws for any reason, the app still launches
/// and Firebase-dependent features fall back to the local Hive cache.
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
  ]);
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.light,
    systemNavigationBarColor: Colors.transparent,
    systemNavigationBarIconBrightness: Brightness.light,
  ));

  await Hive.initFlutter();
  await Hive.openBox<dynamic>('liked_songs');
  await Hive.openBox<dynamic>('recently_played');

  await _initFirebaseSafely();

  // Initialize background audio handler.
  final audioHandler = await AudioService.init(
    builder: () => nocturne_audio.NocturneAudioHandler(),
    config: const AudioServiceConfig(
      androidNotificationChannelId: 'app.nocturne.audio',
      androidNotificationChannelName: 'Nocturne Playback',
      androidNotificationOngoing: true,
      androidStopForegroundOnPause: true,
    ),
  );

  runApp(
    ProviderScope(
      overrides: [
        nocturne_audio.audioHandlerProvider.overrideWithValue(audioHandler),
      ],
      child: const NocturneApp(),
    ),
  );
}

/// Best-effort Firebase init.
///
/// Tries the platform default first (works once `flutterfire configure` has
/// generated `firebase_options.dart` AND it's been wired into this file).
/// If that fails — typically because the project hasn't been wired up yet —
/// we swallow the error and let the app continue without Firebase.
Future<void> _initFirebaseSafely() async {
  try {
    await Firebase.initializeApp();
  } catch (e, st) {
    if (kDebugMode) {
      debugPrint('[Firebase] init skipped: $e');
      debugPrintStack(stackTrace: st, label: 'Firebase init');
    }
  }
}

class NocturneApp extends StatelessWidget {
  const NocturneApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Nocturne',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.dark(),
      home: const RootScreen(),
    );
  }
}
