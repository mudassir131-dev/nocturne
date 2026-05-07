import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:palette_generator/palette_generator.dart';

/// Two-color animated gradient driven by the dominant + accent colors of
/// the current album art. Drops to a subtle fallback while the palette is
/// still loading so the screen never flashes black.
class DynamicGradientBackground extends StatefulWidget {
  final String? artUrl;
  final Widget child;

  const DynamicGradientBackground({
    super.key,
    required this.artUrl,
    required this.child,
  });

  @override
  State<DynamicGradientBackground> createState() =>
      _DynamicGradientBackgroundState();
}

class _DynamicGradientBackgroundState extends State<DynamicGradientBackground> {
  Color _top = const Color(0xFF1F1F1F);
  Color _bottom = const Color(0xFF000000);
  String? _activeUrl;

  @override
  void initState() {
    super.initState();
    _refresh(widget.artUrl);
  }

  @override
  void didUpdateWidget(covariant DynamicGradientBackground oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.artUrl != widget.artUrl) {
      _refresh(widget.artUrl);
    }
  }

  Future<void> _refresh(String? url) async {
    if (url == null || url.isEmpty || url == _activeUrl) return;
    _activeUrl = url;
    try {
      final palette = await PaletteGenerator.fromImageProvider(
        CachedNetworkImageProvider(url),
        size: const Size(180, 180),
      );
      final top = palette.dominantColor?.color ??
          palette.vibrantColor?.color ??
          palette.lightVibrantColor?.color ??
          const Color(0xFF1F1F1F);
      final bottom = palette.darkMutedColor?.color ??
          palette.darkVibrantColor?.color ??
          Color.lerp(top, Colors.black, 0.7) ??
          Colors.black;
      if (mounted && url == _activeUrl) {
        setState(() {
          _top = top;
          _bottom = bottom;
        });
      }
    } catch (_) {/* keep fallback */}
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 850),
      curve: Curves.easeOutCubic,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            _top,
            Color.lerp(_top, _bottom, 0.5) ?? _bottom,
            _bottom,
            Colors.black,
          ],
          stops: const [0.0, 0.45, 0.85, 1.0],
        ),
      ),
      child: widget.child,
    );
  }
}
