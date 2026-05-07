import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/download_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';

class DownloadsScreen extends ConsumerStatefulWidget {
  const DownloadsScreen({super.key});

  @override
  ConsumerState<DownloadsScreen> createState() => _DownloadsScreenState();
}

class _DownloadsScreenState extends ConsumerState<DownloadsScreen> {
  Map<String, double> _progress = {};

  @override
  void initState() {
    super.initState();
    final svc = ref.read(downloadServiceProvider);
    _progress = svc.currentProgress;
    svc.progressStream.listen((p) {
      if (!mounted) return;
      setState(() => _progress = p);
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final svc = ref.watch(downloadServiceProvider);
    final downloads = svc.all();

    return Scaffold(
      appBar: AppBar(title: const Text('Downloads')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(0, 4, 0, 120),
        children: [
          if (_progress.isNotEmpty) ...[
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 8, 20, 8),
              child: Text(
                'In progress',
                style: TextStyle(color: fg, fontWeight: FontWeight.w700),
              ),
            ),
            for (final entry in _progress.entries)
              ListTile(
                leading: const Icon(Icons.download, color: AppColors.accent),
                title: Text(
                  entry.key,
                  style: TextStyle(color: fg, fontFamily: 'monospace'),
                ),
                subtitle: LinearProgressIndicator(
                  value: entry.value,
                  backgroundColor: fg.withOpacity(0.1),
                  valueColor: const AlwaysStoppedAnimation<Color>(AppColors.accent),
                ),
                trailing: Text(
                  '${(entry.value * 100).toStringAsFixed(0)}%',
                  style: TextStyle(color: fg.withOpacity(0.7)),
                ),
              ),
            const Divider(height: 24),
          ],
          if (downloads.isEmpty)
            Padding(
              padding: const EdgeInsets.all(24),
              child: Center(
                child: Text(
                  'No downloaded songs yet.\nTap the download icon on any track to save it for offline.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: fg.withOpacity(0.7)),
                ),
              ),
            )
          else
            ...downloads.map((d) => _DownloadTile(
                  download: d,
                  onPlay: () =>
                      _playOffline(downloads, downloads.indexOf(d)),
                  onDelete: () async {
                    await ref
                        .read(downloadServiceProvider)
                        .delete(d.song.videoId);
                    if (!mounted) return;
                    setState(() {});
                  },
                )),
        ],
      ),
    );
  }

  Future<void> _playOffline(
    List<DownloadedSong> all,
    int startIndex,
  ) async {
    final songs = all.map((d) => d.song).toList();
    await ref
        .read(playerControllerProvider)
        .playQueue(songs, startIndex: startIndex);
  }
}

class _DownloadTile extends StatelessWidget {
  final DownloadedSong download;
  final VoidCallback onPlay;
  final VoidCallback onDelete;

  const _DownloadTile({
    required this.download,
    required this.onPlay,
    required this.onDelete,
  });

  String _fmtSize(int bytes) {
    if (bytes >= 1024 * 1024) {
      return '${(bytes / 1024 / 1024).toStringAsFixed(1)} MB';
    }
    return '${(bytes / 1024).toStringAsFixed(0)} KB';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final s = download.song;
    return ListTile(
      onTap: onPlay,
      leading: ClipRRect(
        borderRadius: BorderRadius.circular(6),
        child: SizedBox(
          width: 44,
          height: 44,
          child: s.thumbnail.isEmpty
              ? Container(
                  color: theme.cardColor,
                  child: Icon(Icons.music_note, color: fg.withOpacity(0.5)),
                )
              : CachedNetworkImage(
                  imageUrl: s.thumbnail,
                  fit: BoxFit.cover,
                  memCacheWidth: 200,
                  memCacheHeight: 200,
                ),
        ),
      ),
      title: Text(
        s.title,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(color: fg, fontWeight: FontWeight.w600),
      ),
      subtitle: Text(
        '${s.artist}  ·  ${_fmtSize(download.sizeBytes)}',
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(color: fg.withOpacity(0.6)),
      ),
      trailing: IconButton(
        icon: Icon(Icons.delete_outline, color: fg.withOpacity(0.7)),
        onPressed: onDelete,
      ),
    );
  }
}
