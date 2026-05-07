import 'dart:async';

import 'package:audio_service/audio_service.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';

import 'firebase_options.dart';
import 'screens/root_screen.dart';
import 'services/audio_service.dart' as nocturne_audio;
import 'utils/theme.dart';

/// The bootstrapper renders a splash immediately and then runs each platform
/// init step inside its own try/catch + timeout. If anything hangs (e.g.
/// `AudioService.init` waiting on a misconfigured foreground service) the
/// rest of the app still launches with a no-op audio handler instead of a
/// black screen.
void main() {
  runZonedGuarded(() {
    WidgetsFlutterBinding.ensureInitialized();

    FlutterError.onError = (details) {
      FlutterError.presentError(details);
      if (kDebugMode) debugPrint('[FlutterError] ${details.exception}');
    };

    runApp(const _BootstrapApp());
  }, (error, stack) {
    debugPrint('[main] uncaught: $error');
    debugPrintStack(stackTrace: stack, label: 'main');
  });
}

class _BootstrapApp extends StatefulWidget {
  const _BootstrapApp();

  @override
  State<_BootstrapApp> createState() => _BootstrapAppState();
}

class _BootstrapAppState extends State<_BootstrapApp> {
  nocturne_audio.NocturneAudioHandler? _audioHandler;
  final List<String> _warnings = [];
  bool _ready = false;

  @override
  void initState() {
    super.initState();
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    await _runStep('SystemChrome', () async {
      await SystemChrome.setPreferredOrientations([
        DeviceOrientation.portraitUp,
      ]);
      // Defer status / nav bar styling to the system; the chrome will be
      // recoloured per-page based on the active brightness once
      // MaterialApp is mounted.
    }, timeoutSeconds: 3);

    await _runStep('Hive', () async {
      await Hive.initFlutter();
      await Hive.openBox<dynamic>('liked_songs');
      await Hive.openBox<dynamic>('recently_played');
      await Hive.openBox<dynamic>('recent_searches');
    }, timeoutSeconds: 5);

    await _runStep('Firebase', () async {
      // Initialize with the generated platform options. The bootstrapper
      // will swallow any error and continue without Firebase, so even if
      // the project ID is misconfigured the app still launches.
      await Firebase.initializeApp(
        options: DefaultFirebaseOptions.currentPlatform,
      );
    }, timeoutSeconds: 5);

    await _runStep('AudioService', () async {
      _audioHandler = await AudioService.init(
        builder: () => nocturne_audio.NocturneAudioHandler(),
        config: const AudioServiceConfig(
          androidNotificationChannelId: 'app.nocturne.audio',
          androidNotificationChannelName: 'Nocturne Playback',
          androidNotificationOngoing: true,
          androidStopForegroundOnPause: true,
          androidShowNotificationBadge: true,
          androidNotificationIcon: 'drawable/ic_stat_nocturne',
        ),
      );
    }, timeoutSeconds: 10);

    if (mounted) {
      setState(() => _ready = true);
    }
  }

  Future<void> _runStep(
    String name,
    Future<void> Function() body, {
    required int timeoutSeconds,
  }) async {
    try {
      await body().timeout(Duration(seconds: timeoutSeconds));
      if (kDebugMode) debugPrint('[bootstrap] $name OK');
    } catch (e, st) {
      _warnings.add('$name: $e');
      if (kDebugMode) {
        debugPrint('[bootstrap] $name FAILED: $e');
        debugPrintStack(stackTrace: st, label: 'bootstrap/$name');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final handler = _audioHandler ?? nocturne_audio.NocturneAudioHandler();
    return ProviderScope(
      overrides: [
        nocturne_audio.audioHandlerProvider.overrideWithValue(handler),
      ],
      child: MaterialApp(
        title: 'Nocturne',
        debugShowCheckedModeBanner: false,
        themeMode: ThemeMode.system,
        theme: AppTheme.light(),
        darkTheme: AppTheme.dark(),
        home: AnimatedSwitcher(
          duration: const Duration(milliseconds: 360),
          switchInCurve: Curves.easeOut,
          switchOutCurve: Curves.easeIn,
          child: _ready
              ? const RootScreen(key: ValueKey('root'))
              : const _SplashScreen(key: ValueKey('splash')),
        ),
        builder: (context, child) {
          // Recolour status / nav bars based on the resolved brightness so
          // they remain readable under both light and dark themes.
          final brightness = Theme.of(context).brightness;
          final iconBrightness = brightness == Brightness.dark
              ? Brightness.light
              : Brightness.dark;
          SystemChrome.setSystemUIOverlayStyle(SystemUiOverlayStyle(
            statusBarColor: Colors.transparent,
            statusBarIconBrightness: iconBrightness,
            systemNavigationBarColor: Colors.transparent,
            systemNavigationBarIconBrightness: iconBrightness,
          ));
          if (_warnings.isEmpty || !kDebugMode) return child!;
          return Stack(children: [
            child!,
            Positioned(
              left: 8,
              right: 8,
              bottom: 8,
              child: Material(
                color: Colors.red.withOpacity(0.85),
                borderRadius: BorderRadius.circular(8),
                child: Padding(
                  padding: const EdgeInsets.all(8),
                  child: Text(
                    'Init warnings:\n${_warnings.join('\n')}',
                    style: const TextStyle(color: Colors.white, fontSize: 11),
                  ),
                ),
              ),
            ),
          ]);
        },
      ),
    );
  }
}

class _SplashScreen extends StatefulWidget {
  const _SplashScreen({super.key});

  @override
  State<_SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<_SplashScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _fade;
  late final Animation<double> _scale;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
    )..forward();
    _fade = CurvedAnimation(parent: _controller, curve: Curves.easeOut);
    _scale = Tween<double>(begin: 0.9, end: 1.0).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeOutCubic),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Center(
        child: FadeTransition(
          opacity: _fade,
          child: ScaleTransition(
            scale: _scale,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Container(
                  width: 140,
                  height: 140,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(28),
                    boxShadow: [
                      BoxShadow(
                        color: AppColors.accent.withOpacity(0.45),
                        blurRadius: 40,
                        spreadRadius: 4,
                      ),
                    ],
                  ),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(28),
                    child: Image.asset(
                      'assets/branding/logo.png',
                      fit: BoxFit.cover,
                    ),
                  ),
                ),
                const SizedBox(height: 24),
                Text(
                  'Nocturne',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 28,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 2,
                    shadows: [
                      Shadow(
                        color: AppColors.accent.withOpacity(0.4),
                        blurRadius: 20,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  AppBranding.tagline,
                  style: TextStyle(
                    color: Colors.white.withOpacity(0.55),
                    fontSize: 12,
                    letterSpacing: 0.5,
                  ),
                ),
                const SizedBox(height: 28),
                const SizedBox(
                  width: 22,
                  height: 22,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    valueColor: AlwaysStoppedAnimation<Color>(AppColors.accent),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
