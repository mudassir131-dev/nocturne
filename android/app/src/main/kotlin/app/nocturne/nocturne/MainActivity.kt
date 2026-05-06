package app.nocturne.nocturne

import io.flutter.embedding.android.FlutterFragmentActivity

// audio_service requires FlutterFragmentActivity (not FlutterActivity) so that
// MediaSessionCompat callbacks can attach to the same FragmentManager the
// plugin uses internally. See: https://pub.dev/packages/audio_service
class MainActivity : FlutterFragmentActivity()
