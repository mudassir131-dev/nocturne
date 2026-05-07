import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/api_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';

class _Station {
  final String name;
  final String tagline;
  final String query;
  final List<Color> gradient;

  const _Station(this.name, this.tagline, this.query, this.gradient);
}

/// Apple-Music-style Radio tab — curated stations that map to backend search
/// queries and start an autoplay queue when tapped.
class RadioScreen extends ConsumerWidget {
  const RadioScreen({super.key});

  static const List<_Station> _stations = [
    _Station('Top Hits Radio', 'The biggest songs right now',
        'top hits 2025', [Color(0xFFE53935), Color(0xFFFF7043)]),
    _Station('Bollywood Radio', 'Latest from Hindi cinema',
        'bollywood new songs', [Color(0xFFFF6F00), Color(0xFFD81B60)]),
    _Station('Lo-fi Beats', 'Chill beats to focus / sleep',
        'lo-fi beats', [Color(0xFF1E88E5), Color(0xFF26A69A)]),
    _Station('Workout Radio', 'High-energy hits',
        'workout playlist', [Color(0xFFEF5350), Color(0xFFFFB300)]),
    _Station('Chill Radio', 'Mellow songs for your evening',
        'chill music', [Color(0xFF7E57C2), Color(0xFF26C6DA)]),
    _Station('Hip Hop Radio', 'Modern rap + classics',
        'hip hop hits', [Color(0xFF6A1B9A), Color(0xFF8E24AA)]),
    _Station('Indie Radio', 'Hand-picked indie tracks',
        'indie music', [Color(0xFF455A64), Color(0xFF78909C)]),
    _Station('Classical Radio', 'Timeless orchestral pieces',
        'classical music', [Color(0xFF263238), Color(0xFF4E342E)]),
    _Station('Jazz Radio', 'Smooth + classic jazz',
        'jazz hits', [Color(0xFF3E2723), Color(0xFF8D6E63)]),
    _Station('Punjabi Radio', 'Latest punjabi bangers',
        'punjabi songs', [Color(0xFF558B2F), Color(0xFFC0CA33)]),
  ];

  Future<void> _startStation(WidgetRef ref, _Station station) async {
    final api = ref.read(apiServiceProvider);
    final songs = await api.search(station.query);
    if (songs.isEmpty) return;
    await ref.read(playerControllerProvider).playQueue(songs);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.only(bottom: 200),
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
            child: Text(
              'Radio',
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSurface,
                fontWeight: FontWeight.w800,
                fontSize: 32,
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 8),
            child: Text(
              'Hand-picked stations powered by your library',
              style: TextStyle(
                color: Theme.of(context).hintColor,
                fontSize: 14,
              ),
            ),
          ),
          const SizedBox(height: 16),
          ..._stations.map(
            (s) => Padding(
              padding: const EdgeInsets.fromLTRB(20, 6, 20, 6),
              child: GestureDetector(
                onTap: () async {
                  await _startStation(ref, s);
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text('Tuning ${s.name}…'),
                        duration: const Duration(seconds: 1),
                      ),
                    );
                  }
                },
                child: Container(
                  height: 96,
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: s.gradient,
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    borderRadius: BorderRadius.circular(AppRadius.card),
                  ),
                  padding:
                      const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
                  child: Row(
                    children: [
                      const Icon(Icons.podcasts,
                          color: Colors.white, size: 36),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(
                              s.name,
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.w800,
                                fontSize: 18,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              s.tagline,
                              style: TextStyle(
                                color: Colors.white.withOpacity(0.85),
                                fontSize: 12,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Container(
                        width: 36,
                        height: 36,
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.25),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.play_arrow,
                            color: Colors.white),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
