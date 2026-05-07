import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';

import '../services/audio_service.dart';
import '../utils/theme.dart';

/// 5-band equalizer + bass boost. Wraps just_audio's [AndroidEqualizer]
/// and [AndroidLoudnessEnhancer] which are wired into the player's
/// [AudioPipeline] in [NocturneAudioHandler].
///
/// "Cinema" preset emulates the wide-stage low-end-forward profile used
/// by Dolby Atmos for headphones (mild V-curve + +6 dB bass boost).
class EqualizerScreen extends ConsumerStatefulWidget {
  const EqualizerScreen({super.key});

  @override
  ConsumerState<EqualizerScreen> createState() => _EqualizerScreenState();
}

class _EqualizerScreenState extends ConsumerState<EqualizerScreen> {
  AndroidEqualizerParameters? _params;
  bool _enabled = true;
  bool _bassBoost = false;
  String _activePreset = 'Flat';
  String? _error;

  static const Map<String, List<double>> _presets = {
    'Flat': [0, 0, 0, 0, 0],
    'Bass Boost': [6, 4, 0, 0, 0],
    'Treble Boost': [0, 0, 0, 4, 6],
    'Vocal': [-2, 0, 4, 4, 0],
    'Rock': [4, 2, -2, 2, 4],
    'Pop': [-1, 2, 4, 2, -1],
    'Jazz': [3, 2, 0, 2, 3],
    'Classical': [4, 3, -2, 3, 4],
    'Cinema': [5, 2, 0, 2, 4], // Dolby Atmos / surround feel
    'Hip Hop': [6, 3, -1, 1, 3],
  };

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final eq = ref.read(audioHandlerProvider).equalizer;
      await eq.setEnabled(_enabled);
      final params = await eq.parameters;
      setState(() => _params = params);
    } catch (e) {
      setState(() => _error = e.toString());
    }
  }

  Future<void> _setEnabled(bool v) async {
    setState(() => _enabled = v);
    try {
      await ref.read(audioHandlerProvider).equalizer.setEnabled(v);
    } catch (_) {/* swallow — equalizer effects can fail on emulators */}
  }

  Future<void> _applyPreset(String name) async {
    final values = _presets[name];
    final params = _params;
    if (values == null || params == null) return;
    setState(() => _activePreset = name);
    for (var i = 0; i < params.bands.length && i < values.length; i++) {
      try {
        await params.bands[i].setGain(values[i]);
      } catch (_) {/* ignore — out-of-range gains clamp on device */}
    }
  }

  Future<void> _setBand(int index, double gain) async {
    final params = _params;
    if (params == null || index >= params.bands.length) return;
    try {
      await params.bands[index].setGain(gain);
      setState(() => _activePreset = 'Custom');
    } catch (_) {/* ignore */}
  }

  Future<void> _setBassBoost(bool v) async {
    setState(() => _bassBoost = v);
    try {
      final l = ref.read(audioHandlerProvider).loudness;
      await l.setEnabled(v);
      // just_audio's loudness API takes dB; +6 dB is a noticeable but
      // non-clipping bass + loudness lift.
      await l.setTargetGain(v ? 6.0 : 0.0);
    } catch (_) {/* ignore */}
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final params = _params;
    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        title: const Text('Equalizer'),
        actions: [
          Switch(
            value: _enabled,
            onChanged: _setEnabled,
            activeColor: theme.colorScheme.primary,
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          children: [
            _GlassCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.surround_sound,
                          color: theme.colorScheme.primary),
                      const SizedBox(width: 8),
                      Text(
                        'Spatial / Bass Boost',
                        style: TextStyle(
                          color: fg,
                          fontWeight: FontWeight.w700,
                          fontSize: 16,
                        ),
                      ),
                      const Spacer(),
                      Switch(
                        value: _bassBoost,
                        onChanged: _setBassBoost,
                        activeColor: theme.colorScheme.primary,
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Adds a wide low-end + loudness lift inspired by Dolby '
                    'Atmos for headphones.',
                    style: TextStyle(
                      color: fg.withOpacity(0.65),
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            _GlassCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Presets',
                    style: TextStyle(
                      color: fg,
                      fontWeight: FontWeight.w700,
                      fontSize: 16,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final name in _presets.keys)
                        _PresetChip(
                          label: name,
                          active: _activePreset == name,
                          onTap: () => _applyPreset(name),
                        ),
                      _PresetChip(
                        label: 'Custom',
                        active: _activePreset == 'Custom',
                        onTap: () {},
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            _GlassCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Bands',
                    style: TextStyle(
                      color: fg,
                      fontWeight: FontWeight.w700,
                      fontSize: 16,
                    ),
                  ),
                  const SizedBox(height: 12),
                  if (_error != null)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 24),
                      child: Text(
                        'Equalizer unavailable on this device.\n($_error)',
                        style: TextStyle(color: fg.withOpacity(0.6)),
                      ),
                    )
                  else if (params == null)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 32),
                      child: Center(
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                    )
                  else
                    SizedBox(
                      height: 280,
                      child: Row(
                        children: [
                          for (var i = 0; i < params.bands.length; i++)
                            Expanded(
                              child: _BandSlider(
                                band: params.bands[i],
                                params: params,
                                onChanged: (v) => _setBand(i, v),
                              ),
                            ),
                        ],
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            Text(
              AppBranding.developer,
              textAlign: TextAlign.center,
              style: TextStyle(
                color: fg.withOpacity(0.5),
                fontSize: 12,
                letterSpacing: 1.2,
              ),
            ),
            const SizedBox(height: 80),
          ],
        ),
      ),
    );
  }
}

class _BandSlider extends StatefulWidget {
  final AndroidEqualizerBand band;
  final AndroidEqualizerParameters params;
  final ValueChanged<double> onChanged;

  const _BandSlider({
    required this.band,
    required this.params,
    required this.onChanged,
  });

  @override
  State<_BandSlider> createState() => _BandSliderState();
}

class _BandSliderState extends State<_BandSlider> {
  late double _value = widget.band.gain;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = theme.colorScheme.onSurface;
    final freq = widget.band.centerFrequency;
    final label = freq >= 1000
        ? '${(freq / 1000).toStringAsFixed(freq.truncateToDouble() % 1000 == 0 ? 0 : 1)}k'
        : freq.toStringAsFixed(0);
    return Column(
      children: [
        SizedBox(
          height: 14,
          child: Text(
            '${_value.toStringAsFixed(0)} dB',
            style: TextStyle(
              color: fg.withOpacity(0.7),
              fontSize: 11,
            ),
          ),
        ),
        Expanded(
          child: RotatedBox(
            quarterTurns: -1,
            child: StreamBuilder<double>(
              stream: widget.band.gainStream,
              builder: (context, snap) {
                final liveValue = snap.data ?? _value;
                return Slider(
                  min: widget.params.minDecibels,
                  max: widget.params.maxDecibels,
                  value: liveValue.clamp(
                    widget.params.minDecibels,
                    widget.params.maxDecibels,
                  ),
                  onChanged: (v) {
                    setState(() => _value = v);
                    widget.onChanged(v);
                  },
                );
              },
            ),
          ),
        ),
        const SizedBox(height: 6),
        Text(
          '${label}Hz',
          style: TextStyle(color: fg.withOpacity(0.85), fontSize: 11),
        ),
      ],
    );
  }
}

class _PresetChip extends StatelessWidget {
  final String label;
  final bool active;
  final VoidCallback onTap;

  const _PresetChip({
    required this.label,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final accent = theme.colorScheme.primary;
    return InkWell(
      borderRadius: BorderRadius.circular(20),
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 220),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: active ? accent : Colors.transparent,
          border: Border.all(
            color: active ? accent : theme.dividerColor.withOpacity(0.5),
          ),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: active ? Colors.white : theme.colorScheme.onSurface,
            fontWeight: FontWeight.w600,
            fontSize: 13,
          ),
        ),
      ),
    );
  }
}

class _GlassCard extends StatelessWidget {
  final Widget child;
  const _GlassCard({required this.child});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    return ClipRRect(
      borderRadius: BorderRadius.circular(AppRadius.card),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: isDark
                ? Colors.white.withOpacity(0.08)
                : Colors.black.withOpacity(0.04),
            border: Border.all(
              color: isDark
                  ? Colors.white.withOpacity(0.18)
                  : Colors.black.withOpacity(0.08),
            ),
            borderRadius: BorderRadius.circular(AppRadius.card),
          ),
          child: child,
        ),
      ),
    );
  }
}
