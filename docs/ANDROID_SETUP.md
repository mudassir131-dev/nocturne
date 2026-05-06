# Android-specific setup

## 1. Generate native folders

This repo ships **only the Dart/Flutter code**. To create the native
`android/` and `ios/` folders, run **once** from the repo root:

```bash
flutter create --platforms=android,ios .
```

This will generate platform projects without overwriting the existing
`lib/`, `pubspec.yaml`, or `assets/`.

## 2. AndroidManifest.xml

After `flutter create`, edit `android/app/src/main/AndroidManifest.xml`
and add the following inside `<manifest>`:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/>
```

Inside `<application>`, register the audio_service service + receiver:

```xml
<service
    android:name="com.ryanheise.audioservice.AudioService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="true">
    <intent-filter>
        <action android:name="android.media.browse.MediaBrowserService"/>
    </intent-filter>
</service>

<receiver
    android:name="com.ryanheise.audioservice.MediaButtonReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MEDIA_BUTTON"/>
    </intent-filter>
</receiver>
```

## 3. Min SDK

Set `minSdkVersion 21` in `android/app/build.gradle` (required by
`firebase_auth` and `just_audio`).

## 4. Firebase

Drop `google-services.json` into `android/app/` and apply the plugin
following [docs/FIREBASE_SETUP.md](FIREBASE_SETUP.md).

## 5. Build

```bash
flutter pub get
flutter run --dart-define=NOCTURNE_BACKEND_URL=https://your-backend.up.railway.app
```
