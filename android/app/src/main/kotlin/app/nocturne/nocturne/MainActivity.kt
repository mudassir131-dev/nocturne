package app.nocturne.nocturne

import com.ryanheise.audioservice.AudioServiceFragmentActivity

// audio_service requires AudioServiceFragmentActivity so the plugin can
// attach to the cached FlutterEngine it manages internally. Extending plain
// FlutterFragmentActivity throws "The Activity class declared in your
// AndroidManifest.xml is wrong or has not provided the correct FlutterEngine"
// at AudioService.init time. See: https://pub.dev/packages/audio_service#android
class MainActivity : AudioServiceFragmentActivity()
