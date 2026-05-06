import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive/hive.dart';

import '../models/song.dart';
import '../utils/config.dart';

final databaseServiceProvider =
    Provider<DatabaseService>((ref) => DatabaseService());

/// Wraps Firestore for cloud sync and Hive boxes for offline access
/// to liked songs and recently-played history.
///
/// All Firestore calls are guarded so that the app continues to work
/// (with local-only state) when Firebase is not yet configured.
class DatabaseService {
  Box<dynamic> get _likedBox => Hive.box<dynamic>('liked_songs');
  Box<dynamic> get _recentBox => Hive.box<dynamic>('recently_played');

  String? get _uid {
    try {
      return FirebaseAuth.instance.currentUser?.uid;
    } catch (_) {
      return null;
    }
  }

  CollectionReference<Map<String, dynamic>>? _userCol(String sub) {
    final uid = _uid;
    if (uid == null) return null;
    try {
      return FirebaseFirestore.instance
          .collection('users')
          .doc(uid)
          .collection(sub);
    } catch (_) {
      return null;
    }
  }

  // ---------- Liked songs ----------

  Future<void> likeSong(Song song) async {
    await _likedBox.put(song.videoId, song.toJson());
    final col = _userCol('liked_songs');
    if (col != null) {
      await col.doc(song.videoId).set({
        ...song.toJson(),
        'addedAt': FieldValue.serverTimestamp(),
      });
    }
  }

  Future<void> unlikeSong(String videoId) async {
    await _likedBox.delete(videoId);
    final col = _userCol('liked_songs');
    if (col != null) {
      await col.doc(videoId).delete();
    }
  }

  bool isLiked(String videoId) => _likedBox.containsKey(videoId);

  /// Read liked songs from local Hive cache (always fast, offline-friendly).
  List<Song> likedSongsLocal() {
    return _likedBox.values
        .whereType<Map>()
        .map((e) => Song.fromJson(Map<String, dynamic>.from(e)))
        .toList();
  }

  Stream<List<Song>> watchLikedSongs() async* {
    yield likedSongsLocal();
    final col = _userCol('liked_songs');
    if (col == null) return;
    yield* col
        .orderBy('addedAt', descending: true)
        .snapshots()
        .map((s) => s.docs.map((d) => Song.fromJson(d.data())).toList());
  }

  // ---------- Recently played ----------

  Future<void> markPlayed(Song song) async {
    final entry = {
      ...song.toJson(),
      'playedAt': DateTime.now().toIso8601String(),
    };
    await _recentBox.put(song.videoId, entry);
    // Keep local cache trimmed.
    if (_recentBox.length > AppConfig.recentlyPlayedLimit) {
      final keys = _recentBox.keys.toList();
      for (var i = 0; i < keys.length - AppConfig.recentlyPlayedLimit; i++) {
        await _recentBox.delete(keys[i]);
      }
    }
    final col = _userCol('recently_played');
    if (col != null) {
      await col.doc(song.videoId).set({
        ...song.toJson(),
        'playedAt': FieldValue.serverTimestamp(),
      });
    }
  }

  List<Song> recentlyPlayedLocal() {
    return _recentBox.values
        .whereType<Map>()
        .map((e) => Song.fromJson(Map<String, dynamic>.from(e)))
        .toList()
        .reversed
        .toList();
  }

  // ---------- Playlists ----------

  Future<String?> createPlaylist(String name, {String coverUrl = ''}) async {
    final col = _userCol('playlists');
    if (col == null) return null;
    final ref = await col.add({
      'name': name,
      'coverUrl': coverUrl,
      'createdAt': FieldValue.serverTimestamp(),
      'songs': <Map<String, dynamic>>[],
    });
    return ref.id;
  }

  Future<void> addSongToPlaylist(String playlistId, Song song) async {
    final col = _userCol('playlists');
    if (col == null) return;
    await col.doc(playlistId).update({
      'songs': FieldValue.arrayUnion([song.toJson()]),
    });
  }

  Stream<List<PlaylistSummary>> watchPlaylists() {
    final col = _userCol('playlists');
    if (col == null) return const Stream.empty();
    return col.orderBy('createdAt', descending: true).snapshots().map(
          (s) => s.docs
              .map((d) => PlaylistSummary.fromDoc(d.id, d.data()))
              .toList(),
        );
  }
}

class PlaylistSummary {
  final String id;
  final String name;
  final String coverUrl;
  final int songCount;

  const PlaylistSummary({
    required this.id,
    required this.name,
    required this.coverUrl,
    required this.songCount,
  });

  factory PlaylistSummary.fromDoc(String id, Map<String, dynamic> data) {
    final songs = data['songs'];
    return PlaylistSummary(
      id: id,
      name: (data['name'] ?? '').toString(),
      coverUrl: (data['coverUrl'] ?? '').toString(),
      songCount: songs is List ? songs.length : 0,
    );
  }
}
