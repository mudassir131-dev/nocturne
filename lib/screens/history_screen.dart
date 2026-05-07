import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/database_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import '../widgets/song_tile.dart';

/// Shows recently played songs (most-recent first). Sourced from the
/// local Hive box so it works offline.
class HistoryScreen extends ConsumerWidget {
  const HistoryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final db = ref.watch(databaseServiceProvider);
    final history = db.recentlyPlayedLocal();

    return Scaffold(
      appBar: AppBar(title: const Text('Listening History')),
      body: history.isEmpty
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  'No history yet.\nTracks you listen to will appear here.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: fg.withOpacity(0.7)),
                ),
              ),
            )
          : ListView.builder(
              padding: const EdgeInsets.fromLTRB(0, 8, 0, 120),
              itemCount: history.length,
              itemBuilder: (_, i) {
                final s = history[i];
                return SongTile(
                  song: s,
                  onTap: () => ref
                      .read(playerControllerProvider)
                      .playQueue(history, startIndex: i),
                  onAdd: () =>
                      ref.read(playerControllerProvider).addToQueue(s),
                  onMenu: () {},
                );
              },
            ),
      bottomNavigationBar: history.isEmpty
          ? null
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.accent,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                  icon: const Icon(Icons.shuffle),
                  label: const Text('Smart Shuffle from History'),
                  onPressed: () async {
                    final ctrl = ref.read(playerControllerProvider);
                    final added = await ctrl.smartShuffle(history.first);
                    if (!context.mounted) return;
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text(added > 0
                            ? 'Built smart queue with $added tracks'
                            : 'Could not build smart queue.'),
                      ),
                    );
                  },
                ),
              ),
            ),
    );
  }
}
