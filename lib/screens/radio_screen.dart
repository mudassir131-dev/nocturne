import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/api_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';

/// `Radio` tab — themed stations that fan out to a yt-dlp seed search and
/// start a continuous queue. Each station card kicks off a queue when
/// tapped.
class RadioScreen extends ConsumerWidget {
  const RadioScreen({super.key});

  static const _stations = <_Station>[
    _Station(
      label: 'Chill Mix',
      seed: 'chill lofi mix',
      gradient: [Color(0xFF7E57C2), Color(0xFF26C6DA)],
      icon: CupertinoIcons.cloud_moon,
    ),
    _Station(
      label: 'Top Hits',
      seed: 'top hits 2025',
      gradient: [Color(0xFFE53935), Color(0xFFFFB300)],
      icon: CupertinoIcons.flame,
    ),
    _Station(
      label: 'Hip-Hop Daily',
      seed: 'hip hop hits',
      gradient: [Color(0xFFFF6F00), Color(0xFFD81B60)],
      icon: CupertinoIcons.music_mic,
    ),
    _Station(
      label: 'Indie / Alt',
      seed: 'indie alt rock playlist',
      gradient: [Color(0xFF1565C0), Color(0xFF26A69A)],
      icon: CupertinoIcons.guitars,
    ),
    _Station(
      label: 'Workout',
      seed: 'workout edm playlist',
      gradient: [Color(0xFF00897B), Color(0xFFCDDC39)],
      icon: CupertinoIcons.bolt,
    ),
    _Station(
      label: 'Sleep',
      seed: 'sleep ambient music',
      gradient: [Color(0xFF1A237E), Color(0xFF263238)],
      icon: CupertinoIcons.moon_stars,
    ),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 200),
        children: [
          Text(
            'Radio',
            style: TextStyle(
              color: fg,
              fontWeight: FontWeight.w800,
              fontSize: 28,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Endless music, curated mixes',
            style: TextStyle(color: fg.withOpacity(0.6), fontSize: 13),
          ),
          const SizedBox(height: 18),
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              mainAxisSpacing: 14,
              crossAxisSpacing: 14,
              childAspectRatio: 1.3,
            ),
            itemCount: _stations.length,
            itemBuilder: (_, i) {
              final s = _stations[i];
              return _StationCard(
                station: s,
                onTap: () => _start(context, ref, s),
              );
            },
          ),
        ],
      ),
    );
  }

  Future<void> _start(BuildContext context, WidgetRef ref, _Station s) async {
    final messenger = ScaffoldMessenger.of(context);
    try {
      final api = ref.read(apiServiceProvider);
      final songs = await api.search(s.seed);
      if (songs.isEmpty) {
        messenger.showSnackBar(
          const SnackBar(content: Text('No tracks available right now.')),
        );
        return;
      }
      await ref.read(playerControllerProvider).playQueue(songs);
    } catch (e) {
      messenger.showSnackBar(
        SnackBar(content: Text('Could not start station: $e')),
      );
    }
  }
}

class _Station {
  final String label;
  final String seed;
  final List<Color> gradient;
  final IconData icon;
  const _Station({
    required this.label,
    required this.seed,
    required this.gradient,
    required this.icon,
  });
}

class _StationCard extends StatelessWidget {
  final _Station station;
  final VoidCallback onTap;
  const _StationCard({required this.station, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(AppRadius.card),
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: station.gradient,
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(AppRadius.card),
        ),
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(station.icon, color: Colors.white, size: 26),
            const Spacer(),
            Text(
              station.label,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w700,
                fontSize: 18,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
