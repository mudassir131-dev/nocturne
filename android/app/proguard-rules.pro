# Flutter wrapper
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.**  { *; }
-keep class io.flutter.util.**  { *; }
-keep class io.flutter.view.**  { *; }
-keep class io.flutter.**  { *; }
-keep class io.flutter.plugins.**  { *; }
-keep class io.flutter.plugin.editing.** { *; }

# audio_service / just_audio
-keep class com.ryanheise.** { *; }
-keep class androidx.media.** { *; }
-keep class androidx.media2.** { *; }

# Firebase (no-op when google-services.json is missing, harmless otherwise)
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Hive (uses reflection on adapters)
-keep class * extends hive.* { *; }
-keepclassmembers class * {
    @hive.HiveField *;
}

# Keep model classes used by JSON parsing
-keepclassmembers class * {
    public <init>(...);
}

# Play Core split-install (only needed for Play Store dynamic delivery; we
# never ship deferred components, so just suppress R8 warnings about it).
-dontwarn com.google.android.play.core.**
-keep class com.google.android.play.core.** { *; }

# Suppress warnings about missing classes referenced indirectly
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
