/// Common song representation used across the app.
class Song {
  final String videoId;
  final String title;
  final String artist;
  final String thumbnail;
  final Duration? duration;

  const Song({
    required this.videoId,
    required this.title,
    required this.artist,
    required this.thumbnail,
    this.duration,
  });

  factory Song.fromJson(Map<String, dynamic> json) {
    final id = (json['id'] ?? json['videoId'] ?? '').toString();
    final rawDuration = json['duration'];
    Duration? dur;
    if (rawDuration is num) {
      dur = Duration(seconds: rawDuration.toInt());
    } else if (rawDuration is String) {
      final parsed = int.tryParse(rawDuration);
      if (parsed != null) dur = Duration(seconds: parsed);
    }

    return Song(
      videoId: id,
      title: (json['title'] ?? '').toString(),
      artist:
          (json['artist'] ?? json['uploader'] ?? json['channel'] ?? 'Unknown')
              .toString(),
      thumbnail: (json['thumbnail'] ?? '').toString(),
      duration: dur,
    );
  }

  Map<String, dynamic> toJson() => {
        'videoId': videoId,
        'title': title,
        'artist': artist,
        'thumbnail': thumbnail,
        'duration': duration?.inSeconds,
      };

  /// Human-readable duration like `3:42`.
  String get durationLabel {
    final d = duration;
    if (d == null) return '';
    final m = d.inMinutes;
    final s = (d.inSeconds % 60).toString().padLeft(2, '0');
    return '$m:$s';
  }

  Song copyWith({
    String? videoId,
    String? title,
    String? artist,
    String? thumbnail,
    Duration? duration,
  }) {
    return Song(
      videoId: videoId ?? this.videoId,
      title: title ?? this.title,
      artist: artist ?? this.artist,
      thumbnail: thumbnail ?? this.thumbnail,
      duration: duration ?? this.duration,
    );
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) || (other is Song && other.videoId == videoId);

  @override
  int get hashCode => videoId.hashCode;
}
