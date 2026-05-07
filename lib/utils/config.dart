import 'dart:convert';

/// Single source of truth for environment-specific configuration.
class AppConfig {
  AppConfig._();

  /// Raw value of the [NOCTURNE_BACKEND_URL] dart-define. May contain
  /// `user:password@` userinfo (used by the dev tunnel). Use
  /// [backendBaseUrl] / [backendAuthHeader] for actual requests.
  static const String _rawBackendUrl = String.fromEnvironment(
    'NOCTURNE_BACKEND_URL',
    defaultValue: 'http://10.0.2.2:3000',
  );

  static final Uri _parsed = Uri.parse(_rawBackendUrl);

  /// Backend URL **without** userinfo. This is what we hand to Dio /
  /// just_audio so platform HTTP stacks (ExoPlayer, AVPlayer, dart:io)
  /// don't choke on a `user:pass@host` form they may not parse.
  static String get backendBaseUrl {
    if (_parsed.userInfo.isEmpty) return _rawBackendUrl;
    return _parsed.replace(userInfo: '').toString();
  }

  /// `"Basic ..."` header value if the configured URL embedded HTTP
  /// basic-auth credentials, else null. Attached to every Dio request
  /// and to streaming `AudioSource.uri` headers so the dev tunnel can
  /// authenticate.
  static String? get backendAuthHeader {
    final ui = _parsed.userInfo;
    if (ui.isEmpty) return null;
    return 'Basic ${base64Encode(utf8.encode(ui))}';
  }

  /// Headers map used by `AudioSource.uri(...)` for streaming. Empty
  /// when no auth is configured.
  static Map<String, String> get streamHeaders {
    final h = backendAuthHeader;
    if (h == null) return const {};
    return {'Authorization': h};
  }

  /// How many recently-played items we retain per user (Firestore + local).
  static const int recentlyPlayedLimit = 30;

  /// Default page size for search results.
  static const int searchPageSize = 20;
}
