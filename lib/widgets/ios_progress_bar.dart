import 'package:flutter/material.dart';

/// iOS 26 style progress bar — a thin track with a soft circular knob that
/// expands on drag and contracts on release. The knob position is animated
/// implicitly so position updates from the player look smooth instead of
/// snapping frame-by-frame.
class IosProgressBar extends StatefulWidget {
  final Duration position;
  final Duration duration;
  final ValueChanged<Duration> onSeek;

  const IosProgressBar({
    super.key,
    required this.position,
    required this.duration,
    required this.onSeek,
  });

  @override
  State<IosProgressBar> createState() => _IosProgressBarState();
}

class _IosProgressBarState extends State<IosProgressBar> {
  bool _scrubbing = false;
  double? _scrubValue; // 0..1

  String _fmt(Duration d) {
    final total = d.inSeconds;
    final m = total ~/ 60;
    final s = (total % 60).toString().padLeft(2, '0');
    return '$m:$s';
  }

  @override
  Widget build(BuildContext context) {
    final totalMs = widget.duration.inMilliseconds.clamp(1, 1 << 31);
    final progress = _scrubValue ??
        (widget.position.inMilliseconds / totalMs).clamp(0.0, 1.0);

    return LayoutBuilder(builder: (context, constraints) {
      final width = constraints.maxWidth;
      void emitSeek(double dx) {
        final clamped = dx.clamp(0.0, width);
        setState(() => _scrubValue = clamped / width);
      }

      void commit() {
        if (_scrubValue != null && widget.duration > Duration.zero) {
          final ms = (_scrubValue! * widget.duration.inMilliseconds).round();
          widget.onSeek(Duration(milliseconds: ms));
        }
        setState(() {
          _scrubbing = false;
          _scrubValue = null;
        });
      }

      return Column(
        children: [
          GestureDetector(
            behavior: HitTestBehavior.opaque,
            onHorizontalDragStart: (d) {
              setState(() => _scrubbing = true);
              emitSeek(d.localPosition.dx);
            },
            onHorizontalDragUpdate: (d) => emitSeek(d.localPosition.dx),
            onHorizontalDragEnd: (_) => commit(),
            onHorizontalDragCancel: commit,
            onTapDown: (d) {
              setState(() => _scrubbing = true);
              emitSeek(d.localPosition.dx);
            },
            onTapUp: (_) => commit(),
            onTapCancel: commit,
            child: SizedBox(
              height: 26,
              width: width,
              child: CustomPaint(
                painter: _IosBarPainter(
                  progress: progress,
                  scrubbing: _scrubbing,
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _label(_fmt(_scrubbing
                    ? Duration(
                        milliseconds:
                            (progress * widget.duration.inMilliseconds).round())
                    : widget.position)),
                _label(widget.duration > Duration.zero
                    ? '-${_fmt(widget.duration - Duration(
                          milliseconds:
                              (progress * widget.duration.inMilliseconds)
                                  .round(),
                        ))}'
                    : '--:--'),
              ],
            ),
          ),
        ],
      );
    });
  }

  Widget _label(String text) {
    return Text(
      text,
      style: TextStyle(
        color: Colors.white.withOpacity(0.6),
        fontSize: 12,
        fontWeight: FontWeight.w500,
        fontFeatures: const [FontFeature.tabularFigures()],
      ),
    );
  }
}

class _IosBarPainter extends CustomPainter {
  final double progress;
  final bool scrubbing;

  _IosBarPainter({required this.progress, required this.scrubbing});

  @override
  void paint(Canvas canvas, Size size) {
    final cy = size.height / 2;
    final trackHeight = scrubbing ? 4.0 : 2.5;
    final trackPaint = Paint()
      ..color = Colors.white.withOpacity(0.25)
      ..strokeCap = StrokeCap.round
      ..strokeWidth = trackHeight;
    final activePaint = Paint()
      ..color = Colors.white
      ..strokeCap = StrokeCap.round
      ..strokeWidth = trackHeight;

    canvas.drawLine(
      Offset(0, cy),
      Offset(size.width, cy),
      trackPaint,
    );
    final activeEnd = (progress.clamp(0.0, 1.0)) * size.width;
    canvas.drawLine(
      Offset(0, cy),
      Offset(activeEnd, cy),
      activePaint,
    );

    // Knob
    final knobR = scrubbing ? 8.0 : 5.5;
    final knobX = activeEnd.clamp(knobR, size.width - knobR);
    final knobShadow = Paint()
      ..color = Colors.black.withOpacity(0.35)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 4);
    canvas.drawCircle(Offset(knobX, cy + 1), knobR, knobShadow);
    canvas.drawCircle(
      Offset(knobX, cy),
      knobR,
      Paint()..color = Colors.white,
    );
  }

  @override
  bool shouldRepaint(covariant _IosBarPainter old) =>
      old.progress != progress || old.scrubbing != scrubbing;
}
