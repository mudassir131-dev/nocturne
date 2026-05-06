# Firebase setup for Nocturne

Nocturne uses Firebase for:

- **Authentication** — Google Sign In (`firebase_auth` + `google_sign_in`).
- **Firestore** — `users/{uid}/liked_songs`, `playlists`, `recently_played`.
- **Storage** _(optional)_ — profile pictures.

The app is designed so it **runs without Firebase configured** — Firebase
calls are guarded and silently fall back to the local Hive cache. Set up
Firebase to enable cross-device sync.

## 1. Create a Firebase project

1. Go to <https://console.firebase.google.com> and create a new project
   called **Nocturne**.
2. Enable **Authentication → Sign-in method → Google**.
3. Create a **Cloud Firestore** database in production mode.

## 2. Add an Android app

1. In Project settings → "Your apps" → add an Android app.
2. Use package name `app.nocturne.app` (or whatever you set in
   `android/app/build.gradle`).
3. Download the generated `google-services.json` and place it at:

   ```
   android/app/google-services.json
   ```

   This file is gitignored on purpose — every developer/dev environment
   should have its own.

## 3. Generate `firebase_options.dart`

Install the FlutterFire CLI once:

```bash
dart pub global activate flutterfire_cli
```

Then, from the repo root:

```bash
flutterfire configure --project=<your-firebase-project-id>
```

This generates `lib/firebase_options.dart`. Once present, wire it into
`lib/main.dart`:

```dart
import 'package:firebase_core/firebase_core.dart';
import 'firebase_options.dart';

await Firebase.initializeApp(
  options: DefaultFirebaseOptions.currentPlatform,
);
```

> The starter `main.dart` intentionally omits `Firebase.initializeApp`
> so the app builds before you've run `flutterfire configure`. Add the
> two lines above as soon as your config is generated.

## 4. Firestore security rules

A minimum-viable ruleset:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null
                         && request.auth.uid == userId;
    }
  }
}
```

## 5. Verify

After signing in via the Profile tab, like a song. You should see a new
document at:

```
users/<uid>/liked_songs/<videoId>
```

If it doesn't appear, check `flutter logs` — the database service logs
permission errors but does not crash the app.
