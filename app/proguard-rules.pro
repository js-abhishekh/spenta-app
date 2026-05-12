# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-keep class com.abhishekhjs.spenta.data.* { *; }

# Google Play Services Nearby
-keep class com.google.android.gms.nearby.connection.** { *; }
-keep interface com.google.android.gms.nearby.connection.** { *; }

# Jetpack Compose
-keep class androidx.compose.material.icons.** { *; }

# Model classes
-keep class com.abhishekhjs.spenta.data.Transaction { *; }
-keep class com.abhishekhjs.spenta.data.Category { *; }