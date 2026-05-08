import 'dart:ui';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../services/lyrics_service.dart';
import '../state/player_provider.dart';
import '../widgets/ios_progress_bar.dart';

/// Full-screen lyrics view. Slides up from the player and renders synced
/// lyrics over a heavy-blur backdrop of the album art.
class LyricsScreen extends ConsumerStatefulWidget {
  final Song song;
  const LyricsScreen({super.key, required this.song});

  @override
  ConsumerState<LyricsScreen> createState() => _LyricsScreenState();
}

class _LyricsScreenState extends ConsumerState<LyricsScreen> {
  late Future<Lyrics> _future;
  final ScrollController _scroll = ScrollController();
  int _activeLine = -1;

  @override
  void initState() {
    super.initState();
    _future = ref.read(lyricsServiceProvider).getLyrics(widget.song);
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final pos = ref.watch(positionProvider).value ?? Duration.zero;
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          if (widget.song.thumbnail.isNotEmpty)
            Positioned.fill(
              child: ImageFiltered(
                imageFilter: ImageFilter.blur(sigmaX: 50, sigmaY: 50),
                child: CachedNetworkImage(
                  imageUrl: widget.song.thumbnail,
                  fit: BoxFit.cover,
                ),
              ),
            ),
          Positioned.fill(
            child: Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    Colors.black.withOpacity(0.55),
                    Colors.black.withOpacity(0.85),
                  ],
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                ),
              ),
            ),
          ),
          SafeArea(
            child: GestureDetector(
              behavior: HitTestBehavior.translucent,
              onVerticalDragEnd: (d) {
                if ((d.primaryVelocity ?? 0) > 250) {
                  Navigator.of(context).maybePop();
                }
              },
              child: Column(
                children: [
                  _Header(song: widget.song),
                  const _DotIndicators(activePage: 0),
                  Expanded(
                    child: FutureBuilder<Lyrics>(
                      future: _future,
                      builder: (context, snap) {
                        final lyrics = snap.data;
                        if (snap.connectionState == ConnectionState.waiting) {
                          return const Center(
                            child: CircularProgressIndicator(
                              color: Colors.white70,
                              strokeWidth: 2,
                            ),
                          );
                        }
                        if (lyrics == null || lyrics.isEmpty) {
                          return const Center(
                            child: Padding(
                              padding: EdgeInsets.all(24),
                              child: Text(
                                "We couldn't find lyrics for this track.",
                                textAlign: TextAlign.center,
                                style: TextStyle(
                                  color: Colors.white70,
                                  fontSize: 15,
                                ),
                              ),
                            ),
                          );
                        }
                        return _LyricsList(
                          lyrics: lyrics,
                          position: pos,
                          scroll: _scroll,
                          onActiveLineChanged: (i) => _activeLine = i,
                          onSeek: (p) =>
                              ref.read(playerControllerProvider).seek(p),
                        );
                      },
                    ),
                  ),
                  _LyricsBottomBar(),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Header extends StatelessWidget {
  final Song song;
  const _Header({required this.song});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 6),
      child: Row(
        children: [
          if (song.thumbnail.isNotEmpty)
            ClipRRect(
              borderRadius: BorderRadius.circular(6),
              child: CachedNetworkImage(
                imageUrl: song.thumbnail,
                width: 36,
                height: 36,
                fit: BoxFit.cover,
              ),
            )
          else
            const SizedBox(width: 36, height: 36),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  song.title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                Text(
                  song.artist,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: Colors.white.withOpacity(0.65),
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            icon: const Icon(CupertinoIcons.xmark, color: Colors.white),
            onPressed: () => Navigator.of(context).maybePop(),
          ),
        ],
      ),
    );
  }
}

/// Three-dot “swipeable indicators” displayed under the lyrics header,
/// matching the iOS 26 lyrics presentation.
class _DotIndicators extends StatelessWidget {
  final int activePage;
  const _DotIndicators({required this.activePage});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: List.generate(3, (i) {
          final active = i == activePage;
          return AnimatedContainer(
            duration: const Duration(milliseconds: 220),
            margin: const EdgeInsets.symmetric(horizontal: 3),
            width: active ? 14 : 6,
            height: 6,
            decoration: BoxDecoration(
              color: active
                  ? Colors.white
                  : Colors.white.withOpacity(0.35),
              borderRadius: BorderRadius.circular(3),
            ),
          );
        }),
      ),
    );
  }
}

/// Mini progress bar + Lossless badge pinned to the bottom of the
/// lyrics screen.
class _LyricsBottomBar extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final pos = ref.watch(positionProvider).value ?? Duration.zero;
    final dur = ref.watch(durationProvider).value ?? Duration.zero;
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 0, 20, 18),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          IosProgressBar(
            position: pos,
            duration: dur,
            onSeek: (p) => ref.read(playerControllerProvider).seek(p),
          ),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.10),
              borderRadius: BorderRadius.circular(4),
              border: Border.all(color: Colors.white.withOpacity(0.18)),
            ),
            child: const Text(
              'LOSSLESS',
              style: TextStyle(
                color: Colors.white,
                fontSize: 9,
                fontWeight: FontWeight.w700,
                letterSpacing: 1.4,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _LyricsList extends StatefulWidget {
  final Lyrics lyrics;
  final Duration position;
  final ScrollController scroll;
  final ValueChanged<int> onActiveLineChanged;
  final ValueChanged<Duration> onSeek;

  const _LyricsList({
    required this.lyrics,
    required this.position,
    required this.scroll,
    required this.onActiveLineChanged,
    required this.onSeek,
  });

  @override
  State<_LyricsList> createState() => _LyricsListState();
}

class _LyricsListState extends State<_LyricsList> {
  int _active = -1;
  static const double _lineHeight = 56;

  @override
  void didUpdateWidget(covariant _LyricsList oldWidget) {
    super.didUpdateWidget(oldWidget);
    final next = _findActive(widget.position);
    if (next != _active) {
      _active = next;
      widget.onActiveLineChanged(next);
      _scrollToActive();
    }
  }

  int _findActive(Duration position) {
    if (!widget.lyrics.synced) return -1;
    final lines = widget.lyrics.lines;
    var lo = 0;
    var hi = lines.length - 1;
    var match = -1;
    while (lo <= hi) {
      final mid = (lo + hi) >> 1;
      if (lines[mid].time <= position) {
        match = mid;
        lo = mid + 1;
      } else {
        hi = mid - 1;
      }
    }
    return match;
  }

  void _scrollToActive() {
    if (_active < 0 || !widget.scroll.hasClients) return;
    final viewportH = widget.scroll.position.viewportDimension;
    final offset =
        (_active * _lineHeight) - (viewportH / 2) + (_lineHeight / 2);
    widget.scroll.animateTo(
      offset.clamp(0.0, widget.scroll.position.maxScrollExtent),
      duration: const Duration(milliseconds: 420),
      curve: Curves.easeOutCubic,
    );
  }

  @override
  Widget build(BuildContext context) {
    final lines = widget.lyrics.lines;
    final synced = widget.lyrics.synced;
    return ListView.builder(
      controller: widget.scroll,
      padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 60),
      itemCount: lines.length,
      itemExtent: _lineHeight,
      itemBuilder: (_, i) {
        final isActive = i == _active;
        // iOS 26 opacity tiers: current = 100%, past = 25%, future = 40%.
        final isPast = synced && _active >= 0 && i < _active;
        final opacity = isActive
            ? 1.0
            : isPast
                ? 0.25
                : 0.40;
        return GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: synced ? () => widget.onSeek(lines[i].time) : null,
          child: Center(
            child: AnimatedDefaultTextStyle(
              duration: const Duration(milliseconds: 240),
              style: TextStyle(
                color: Colors.white.withOpacity(opacity),
                fontWeight: isActive ? FontWeight.w800 : FontWeight.w600,
                fontSize: isActive ? 24 : 19,
                height: 1.25,
              ),
              child: Text(
                lines[i].text,
                textAlign: TextAlign.center,
              ),
            ),
          ),
        );
      },
    );
  }
}
