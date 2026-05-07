import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../state/player_provider.dart';
import '../utils/theme.dart';

class SleepTimerScreen extends ConsumerStatefulWidget {
  const SleepTimerScreen({super.key});

  @override
  ConsumerState<SleepTimerScreen> createState() => _SleepTimerScreenState();
}

class _SleepTimerScreenState extends ConsumerState<SleepTimerScreen> {
  Timer? _ticker;

  static const _options = <int>[15, 30, 45, 60, 90];

  @override
  void initState() {
    super.initState();
    _ticker = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _ticker?.cancel();
    super.dispose();
  }

  String _format(Duration d) {
    final m = d.inMinutes.remainder(60).toString().padLeft(2, '0');
    final s = d.inSeconds.remainder(60).toString().padLeft(2, '0');
    final h = d.inHours;
    if (h > 0) return '${h.toString().padLeft(2, '0')}:$m:$s';
    return '$m:$s';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final fires = ref.watch(sleepTimerProvider).value;
    final remaining = fires?.difference(DateTime.now());
    final active = remaining != null && remaining.inSeconds > 0;

    return Scaffold(
      appBar: AppBar(title: const Text('Sleep Timer')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 80),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(vertical: 24, horizontal: 16),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    theme.colorScheme.primary.withOpacity(0.15),
                    theme.colorScheme.primary.withOpacity(0.04),
                  ],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(AppRadius.card),
                border: Border.all(
                  color: theme.colorScheme.primary.withOpacity(0.25),
                ),
              ),
              child: Column(
                children: [
                  Icon(
                    active ? Icons.bedtime : Icons.bedtime_outlined,
                    color: theme.colorScheme.primary,
                    size: 48,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    active ? _format(remaining) : 'No timer running',
                    style: TextStyle(
                      color: fg,
                      fontWeight: FontWeight.w800,
                      fontSize: active ? 36 : 22,
                      fontFeatures: const [FontFeature.tabularFigures()],
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    active
                        ? 'Music will fade out and pause.'
                        : 'Pick a duration below to schedule a pause.',
                    style: TextStyle(color: fg.withOpacity(0.65)),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'Quick options',
              style: TextStyle(
                color: fg,
                fontWeight: FontWeight.w700,
                fontSize: 18,
              ),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                for (final m in _options)
                  ChoiceChip(
                    label: Text('$m min'),
                    selected: false,
                    onSelected: (_) {
                      ref
                          .read(playerControllerProvider)
                          .setSleepTimer(Duration(minutes: m));
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text('Sleep timer set: $m min'),
                          duration: const Duration(seconds: 1),
                        ),
                      );
                    },
                  ),
              ],
            ),
            const SizedBox(height: 24),
            if (active)
              OutlinedButton.icon(
                icon: const Icon(Icons.cancel_outlined),
                label: const Text('Cancel timer'),
                onPressed: () {
                  ref.read(playerControllerProvider).setSleepTimer(null);
                },
              ),
          ],
        ),
      ),
    );
  }
}
