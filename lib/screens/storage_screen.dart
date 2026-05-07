import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../services/download_service.dart';
import '../services/settings_service.dart';
import '../utils/theme.dart';

class StorageScreen extends ConsumerStatefulWidget {
  const StorageScreen({super.key});

  @override
  ConsumerState<StorageScreen> createState() => _StorageScreenState();
}

class _StorageScreenState extends ConsumerState<StorageScreen> {
  int _bytes = 0;
  int _quality = 320;

  @override
  void initState() {
    super.initState();
    _load();
    _quality = SettingsService.instance.downloadQualityKbps;
  }

  Future<void> _load() async {
    final svc = ref.read(downloadServiceProvider);
    final b = await svc.totalSizeBytes();
    if (!mounted) return;
    setState(() => _bytes = b);
  }

  String _fmtSize(int bytes) {
    if (bytes >= 1024 * 1024 * 1024) {
      return '${(bytes / 1024 / 1024 / 1024).toStringAsFixed(2)} GB';
    }
    if (bytes >= 1024 * 1024) {
      return '${(bytes / 1024 / 1024).toStringAsFixed(1)} MB';
    }
    if (bytes >= 1024) return '${(bytes / 1024).toStringAsFixed(0)} KB';
    return '$bytes B';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final svc = ref.watch(downloadServiceProvider);
    final count = svc.all().length;

    return Scaffold(
      appBar: AppBar(title: const Text('Storage')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 80),
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: theme.cardColor,
              borderRadius: BorderRadius.circular(AppRadius.card),
              border: Border.all(color: fg.withOpacity(0.08)),
            ),
            child: Row(
              children: [
                Icon(Icons.sd_card, color: theme.colorScheme.primary, size: 36),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Downloaded music',
                        style: TextStyle(
                          color: fg,
                          fontWeight: FontWeight.w700,
                          fontSize: 16,
                        ),
                      ),
                      Text(
                        '$count song${count == 1 ? '' : 's'}  ·  ${_fmtSize(_bytes)}',
                        style: TextStyle(color: fg.withOpacity(0.6)),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          Text(
            'Download quality',
            style: TextStyle(
              color: fg,
              fontWeight: FontWeight.w700,
              fontSize: 16,
            ),
          ),
          const SizedBox(height: 8),
          for (final q in const [128, 320])
            RadioListTile<int>(
              title: Text(
                '$q kbps',
                style: TextStyle(color: fg),
              ),
              subtitle: Text(
                q == 128
                    ? 'Smaller files (~3 MB / song)'
                    : 'Best quality (~7-9 MB / song)',
                style: TextStyle(color: fg.withOpacity(0.6)),
              ),
              activeColor: theme.colorScheme.primary,
              value: q,
              groupValue: _quality,
              onChanged: (v) async {
                if (v == null) return;
                setState(() => _quality = v);
                await SettingsService.instance.setDownloadQualityKbps(v);
              },
            ),
          const SizedBox(height: 24),
          OutlinedButton.icon(
            icon: const Icon(Icons.delete_sweep_outlined,
                color: AppColors.accent),
            label: const Text(
              'Clear all downloads',
              style: TextStyle(color: AppColors.accent),
            ),
            onPressed: count == 0
                ? null
                : () async {
                    final confirm = await showDialog<bool>(
                      context: context,
                      builder: (ctx) => AlertDialog(
                        backgroundColor: theme.cardColor,
                        title: Text(
                          'Delete all downloads?',
                          style: TextStyle(color: fg),
                        ),
                        content: Text(
                          'This will free ${_fmtSize(_bytes)} of storage.',
                          style: TextStyle(color: fg.withOpacity(0.8)),
                        ),
                        actions: [
                          TextButton(
                            onPressed: () => Navigator.of(ctx).pop(false),
                            child: const Text('Cancel'),
                          ),
                          TextButton(
                            onPressed: () => Navigator.of(ctx).pop(true),
                            child: const Text(
                              'Delete',
                              style: TextStyle(color: AppColors.accent),
                            ),
                          ),
                        ],
                      ),
                    );
                    if (confirm == true) {
                      await ref.read(downloadServiceProvider).deleteAll();
                      await _load();
                    }
                  },
          ),
          const SizedBox(height: 24),
          OutlinedButton.icon(
            icon: Icon(Icons.cleaning_services_outlined,
                color: fg.withOpacity(0.8)),
            label: Text(
              'Clear cached app data',
              style: TextStyle(color: fg.withOpacity(0.85)),
            ),
            onPressed: () async {
              final p = await SharedPreferences.getInstance();
              await p.remove('last_song_json');
              await p.remove('last_position_ms');
              if (!context.mounted) return;
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Cached app state cleared.')),
              );
            },
          ),
        ],
      ),
    );
  }
}
