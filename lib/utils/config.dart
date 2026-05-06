/// Single source of truth for environment-specific configuration.
///
/// Update [backendBaseUrl] to point to your deployed Node.js backend
/// (e.g. https://nocturne-backend.up.railway.app). All API and audio
/// streaming requests in the app derive their URL from this value.
class AppConfig {
  AppConfig._();

  /// Base URL of the Nocturne Node.js backend.
  /// Override at build time with:
  ///   flutter run --dart-define=NOCTURNE_BACKEND_URL=https://your-backend
  static const String backendBaseUrl = String.fromEnvironment(
    'NOCTURNE_BACKEND_URL',
    defaultValue: 'http://10.0.2.2:3000',
  );

  /// How many recently-played items we retain per user (Firestore + local).
  static const int recentlyPlayedLimit = 30;

  /// Default page size for search results.
  static const int searchPageSize = 20;
}
