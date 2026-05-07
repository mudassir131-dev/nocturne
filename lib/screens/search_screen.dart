import 'dart:async';
import 'dart:ui';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';

import '../models/song.dart';
import '../services/api_service.dart';
import '../state/player_provider.dart';
import '../utils/theme.dart';
import '../widgets/liquid_glass_searchbar.dart';
import '../widgets/song_tile.dart';

class _SearchState {
  final String query;
  final bool loading;
  final List<Song> results;
  final String? error;

  const _SearchState({
    this.query = '',
    this.loading = false,
    this.results = const [],
    this.error,
  });

  _SearchState copyWith({
    String? query,
    bool? loading,
    List<Song>? results,
    String? error,
  }) {
    return _SearchState(
      query: query ?? this.query,
      loading: loading ?? this.loading,
      results: results ?? this.results,
      error: error,
    );
  }
}

class _SearchNotifier extends StateNotifier<_SearchState> {
  _SearchNotifier(this._ref) : super(const _SearchState());
  final Ref _ref;
  Timer? _debounce;

  void onChanged(String q) {
    _debounce?.cancel();
    state = state.copyWith(query: q);
    if (q.trim().isEmpty) {
      state = state.copyWith(results: const [], loading: false, error: null);
      return;
    }
    _debounce = Timer(const Duration(milliseconds: 350), () => _run(q));
  }

  Future<void> _run(String q) async {
    state = state.copyWith(loading: true, error: null);
    try {
      final api = _ref.read(apiServiceProvider);
      final res = await api.search(q);
      state = state.copyWith(loading: false, results: res);
      _RecentSearches.push(q);
    } catch (e) {
      state = state.copyWith(loading: false, error: e.toString());
    }
  }

  @override
  void dispose() {
    _debounce?.cancel();
    super.dispose();
  }
}

final _searchProvider =
    StateNotifierProvider.autoDispose<_SearchNotifier, _SearchState>(
  (ref) => _SearchNotifier(ref),
);

/// Filter tab on the results page.
enum _ResultFilter { top, songs, albums, artists, playlists }

extension on _ResultFilter {
  String get label => switch (this) {
        _ResultFilter.top => 'Top',
        _ResultFilter.songs => 'Songs',
        _ResultFilter.albums => 'Albums',
        _ResultFilter.artists => 'Artists',
        _ResultFilter.playlists => 'Playlists',
      };
}

class SearchScreen extends ConsumerStatefulWidget {
  const SearchScreen({super.key});

  @override
  ConsumerState<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends ConsumerState<SearchScreen> {
  final _controller = TextEditingController();
  _ResultFilter _filter = _ResultFilter.top;

  /// Six "Browse Categories" gradient cards as specified.
  static const _categories = <_Category>[
    _Category(
      'Pop',
      [Color(0xFFEC407A), Color(0xFFF06292)],
      CupertinoIcons.music_mic,
    ),
    _Category(
      'Hip-Hop',
      [Color(0xFFFF7043), Color(0xFFFFA726)],
      CupertinoIcons.headphones,
    ),
    _Category(
      'Rock',
      [Color(0xFFE53935), Color(0xFFB71C1C)],
      CupertinoIcons.music_note_2,
    ),
    _Category(
      'Lo-Fi',
      [Color(0xFF7E57C2), Color(0xFF5E35B1)],
      CupertinoIcons.cloud_moon,
    ),
    _Category(
      'Trending',
      [Color(0xFF1E88E5), Color(0xFF26C6DA)],
      CupertinoIcons.flame,
    ),
    _Category(
      'Desi',
      [Color(0xFF43A047), Color(0xFF2E7D32)],
      CupertinoIcons.star_circle,
    ),
  ];

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(_searchProvider);
    final notifier = ref.read(_searchProvider.notifier);

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Search',
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSurface,
                fontWeight: FontWeight.w800,
                fontSize: 28,
              ),
            ),
            const SizedBox(height: 16),
            LiquidGlassSearchBar(
              controller: _controller,
              onChanged: notifier.onChanged,
              trailing: Icon(
                CupertinoIcons.mic_fill,
                color: Theme.of(context).hintColor,
              ),
            ),
            const SizedBox(height: 16),
            Expanded(
              child: state.query.isEmpty
                  ? _Browse(
                      categories: _categories,
                      onPick: (label) {
                        _controller.text = label;
                        notifier.onChanged(label);
                      },
                    )
                  : Column(
                      children: [
                        _FilterTabs(
                          filter: _filter,
                          onChanged: (f) => setState(() => _filter = f),
                        ),
                        const SizedBox(height: 8),
                        Expanded(
                          child: _Results(state: state, filter: _filter),
                        ),
                      ],
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FilterTabs extends StatelessWidget {
  final _ResultFilter filter;
  final ValueChanged<_ResultFilter> onChanged;
  const _FilterTabs({required this.filter, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (final f in _ResultFilter.values)
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: _FilterChip(
                label: f.label,
                active: filter == f,
                onTap: () => onChanged(f),
              ),
            ),
        ],
      ),
    );
  }
}

class _FilterChip extends StatelessWidget {
  final String label;
  final bool active;
  final VoidCallback onTap;
  const _FilterChip({
    required this.label,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(20),
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
        decoration: BoxDecoration(
          color: active ? AppColors.accent : Colors.white.withOpacity(0.08),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: active
                ? AppColors.accent
                : Colors.white.withOpacity(0.18),
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: active ? Colors.white : Colors.white.withOpacity(0.8),
            fontWeight: FontWeight.w700,
            fontSize: 13,
          ),
        ),
      ),
    );
  }
}

class _Browse extends StatelessWidget {
  final List<_Category> categories;
  final ValueChanged<String> onPick;

  const _Browse({required this.categories, required this.onPick});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<Box<dynamic>>(
      valueListenable: _RecentSearches.box.listenable(),
      builder: (context, box, _) {
        final recents = _RecentSearches.list();
        return ListView(
          padding: const EdgeInsets.only(bottom: 180),
          children: [
            if (recents.isNotEmpty) ...[
              _SectionHeader(
                title: 'Recent',
                onClear: _RecentSearches.clear,
              ),
              const SizedBox(height: 8),
              _RecentChips(items: recents, onTap: onPick),
              const SizedBox(height: 20),
            ],
            const _SectionHeader(title: 'Browse Categories'),
            const SizedBox(height: 8),
            GridView.builder(
              physics: const NeverScrollableScrollPhysics(),
              shrinkWrap: true,
              itemCount: categories.length,
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
                childAspectRatio: 1.5,
              ),
              itemBuilder: (_, i) {
                final c = categories[i];
                return GestureDetector(
                  onTap: () => onPick(c.label),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(AppRadius.card),
                    child: BackdropFilter(
                      filter: ImageFilter.blur(sigmaX: 12, sigmaY: 12),
                      child: Container(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: c.gradient,
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                          ),
                          borderRadius:
                              BorderRadius.circular(AppRadius.card),
                          border: Border.all(
                            color: Colors.white.withOpacity(0.20),
                            width: 1,
                          ),
                        ),
                        padding: const EdgeInsets.all(14),
                        child: Stack(
                          children: [
                            Positioned(
                              top: 0,
                              right: 0,
                              child: Icon(
                                c.icon,
                                color: Colors.white.withOpacity(0.85),
                                size: 26,
                              ),
                            ),
                            Align(
                              alignment: Alignment.bottomLeft,
                              child: Text(
                                c.label,
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.w800,
                                  fontSize: 18,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ],
        );
      },
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  final VoidCallback? onClear;
  const _SectionHeader({required this.title, this.onClear});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Text(
            title,
            style: TextStyle(
              color: Theme.of(context).colorScheme.onSurface,
              fontWeight: FontWeight.w700,
              fontSize: 18,
            ),
          ),
        ),
        if (onClear != null)
          TextButton(
            onPressed: onClear,
            style: TextButton.styleFrom(
              foregroundColor: Theme.of(context).hintColor,
            ),
            child: const Text('Clear'),
          ),
      ],
    );
  }
}

class _RecentChips extends StatelessWidget {
  final List<String> items;
  final ValueChanged<String> onTap;

  const _RecentChips({required this.items, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final fg = Theme.of(context).colorScheme.onSurface;
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: items.map((q) {
        return InkWell(
          borderRadius: BorderRadius.circular(20),
          onTap: () => onTap(q),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            decoration: BoxDecoration(
              color: fg.withOpacity(0.08),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: fg.withOpacity(0.18)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(CupertinoIcons.clock,
                    size: 14, color: fg.withOpacity(0.6)),
                const SizedBox(width: 6),
                Text(
                  q,
                  style: TextStyle(
                    color: fg,
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(width: 6),
                GestureDetector(
                  onTap: () => _RecentSearches.remove(q),
                  child: Icon(CupertinoIcons.xmark,
                      size: 14, color: fg.withOpacity(0.5)),
                ),
              ],
            ),
          ),
        );
      }).toList(),
    );
  }
}

class _Category {
  final String label;
  final List<Color> gradient;
  final IconData icon;
  const _Category(this.label, this.gradient, this.icon);
}

class _Results extends ConsumerWidget {
  final _SearchState state;
  final _ResultFilter filter;
  const _Results({required this.state, required this.filter});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (state.loading) {
      return const Center(
        child: CircularProgressIndicator(color: AppColors.accent),
      );
    }
    if (state.error != null) {
      return Padding(
        padding: const EdgeInsets.all(20),
        child: Text(
          'Search failed.\n${state.error}',
          style: TextStyle(color: Theme.of(context).hintColor),
        ),
      );
    }
    if (state.results.isEmpty) {
      return Center(
        child: Text(
          'No results',
          style: TextStyle(color: Theme.of(context).hintColor),
        ),
      );
    }
    // The backend returns videos only; the Songs/Albums/Artists/Playlists
    // filters are surfaced for parity with iOS but currently render the
    // same flat song list. Once the backend grows those resource types
    // we can switch on `filter` here.
    return ListView.builder(
      padding: const EdgeInsets.only(bottom: 180),
      itemCount: state.results.length,
      itemBuilder: (_, i) {
        final s = state.results[i];
        return SongTile(
          song: s,
          onTap: () => ref
              .read(playerControllerProvider)
              .playQueue(state.results, startIndex: i),
          onMenu: () {},
        );
      },
    );
  }
}

/// Persisted recent-search history. Backed by the `recent_searches` Hive
/// box (lazily opened on first access). Capped at 12 entries.
class _RecentSearches {
  static const int _limit = 12;
  static const String _boxName = 'recent_searches';

  static Box<dynamic> get box {
    if (!Hive.isBoxOpen(_boxName)) {
      Hive.openBox<dynamic>(_boxName);
    }
    return Hive.box<dynamic>(_boxName);
  }

  static List<String> list() {
    if (!Hive.isBoxOpen(_boxName)) return const [];
    final raw = Hive.box<dynamic>(_boxName).get('items');
    if (raw is List) {
      return raw.map((e) => e.toString()).toList(growable: false);
    }
    return const [];
  }

  static Future<void> push(String query) async {
    final q = query.trim();
    if (q.isEmpty) return;
    if (!Hive.isBoxOpen(_boxName)) {
      await Hive.openBox<dynamic>(_boxName);
    }
    final current = list().toList();
    current.removeWhere((e) => e.toLowerCase() == q.toLowerCase());
    current.insert(0, q);
    if (current.length > _limit) {
      current.removeRange(_limit, current.length);
    }
    await Hive.box<dynamic>(_boxName).put('items', current);
  }

  static Future<void> remove(String query) async {
    if (!Hive.isBoxOpen(_boxName)) return;
    final current = list().toList()..removeWhere((e) => e == query);
    await Hive.box<dynamic>(_boxName).put('items', current);
  }

  static Future<void> clear() async {
    if (!Hive.isBoxOpen(_boxName)) return;
    await Hive.box<dynamic>(_boxName).delete('items');
  }
}
