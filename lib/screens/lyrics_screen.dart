import 'dart:ui';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/song.dart';
import '../services/lyrics_service.dart';
import '../utils/theme.dart';

/// Full-screen lyrics view with a blurred album-art backdrop and white
/// text. Lyrics are fetched from lyrics.ovh — works without an API key
/// for most popular tracks.
class LyricsScreen extends ConsumerStatefulWidget {
  final Song song;
  const LyricsScreen({super.key, required this.song});

  @override
  ConsumerState<LyricsScreen> createState() => _LyricsScreenState();
}

class _LyricsScreenState extends ConsumerState<LyricsScreen> {
  String? _lyrics;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final lyrics = await ref.read(lyricsServiceProvider).fetch(widget.song);
      if (!mounted) return;
      setState(() {
        _lyrics = lyrics;
        _loading = false;
        _error = lyrics == null
            ? 'No lyrics found for this track.'
            : null;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Could not load lyrics: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final song = widget.song;
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        fit: StackFit.expand,
        children: [
          if (song.thumbnail.isNotEmpty)
            CachedNetworkImage(
              imageUrl: song.thumbnail,
              fit: BoxFit.cover,
              memCacheWidth: 720,
              memCacheHeight: 720,
            ),
          BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 35, sigmaY: 35),
            child: Container(color: Colors.black.withOpacity(0.55)),
          ),
          SafeArea(
            child: Column(
              children: [
                Row(
                  children: [
                    IconButton(
                      icon: const Icon(Icons.keyboard_arrow_down,
                          color: Colors.white, size: 32),
                      onPressed: () => Navigator.of(context).maybePop(),
                    ),
                    const Spacer(),
                    const Text(
                      'Lyrics',
                      style: TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const Spacer(),
                    const SizedBox(width: 48),
                  ],
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        song.title,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 22,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        song.artist,
                        style: const TextStyle(color: Colors.white70),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                Expanded(child: _Body(
                  loading: _loading,
                  error: _error,
                  lyrics: _lyrics,
                )),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Body extends StatelessWidget {
  final bool loading;
  final String? error;
  final String? lyrics;
  const _Body({required this.loading, required this.error, required this.lyrics});

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const Center(
        child: CircularProgressIndicator(
          valueColor: AlwaysStoppedAnimation<Color>(AppColors.accent),
        ),
      );
    }
    if (error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            error!,
            textAlign: TextAlign.center,
            style: const TextStyle(color: Colors.white70, fontSize: 15),
          ),
        ),
      );
    }
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 80),
      child: Text(
        lyrics ?? '',
        style: const TextStyle(
          color: Colors.white,
          fontSize: 16,
          height: 1.55,
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }
}
