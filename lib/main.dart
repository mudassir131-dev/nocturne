import 'package:audio_service/audio_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';

import 'screens/root_screen.dart';
import 'services/audio_service.dart' as nocturne_audio;
import 'utils/theme.dart';

/// Entry point. Initializes Hive (offline cache) and the AudioService
/// background-audio handler, then runs the app.
///
/// Firebase initialization is intentionally optional: the app will run
/// without Firebase configured, and Firebase-dependent features will
/// surface a friendly error/empty state instead of crashing.
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
