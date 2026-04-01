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

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Keep all classes used by Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Firebase Analytics
-keep class com.google.android.gms.measurement.** { *; }

# AppUpdateManager (in-app updates)
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

# Glide configuration
-keep public class * implements com.bumptech.glide.module.GlideModule

-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Shimmer
-keep class com.facebook.shimmer.** { *; }
-dontwarn com.facebook.shimmer.**

# Android-SpinKit
-keep class com.github.ybq.** { *; }
-dontwarn com.github.ybq.**

# PinView
-keep class io.github.chaosleung.pinview.** { *; }
-dontwarn io.github.chaosleung.pinview.**

# NafisBottomNav library
-keep class com.foysaldev.** { *; }
-dontwarn com.foysaldev.**

# ViewBinding and DataBinding
-keep class **Binding { *; }
-keep class **Databinding { *; }

# Keep custom widgets
-keep class com.sourav.livebusgietu.TrackBuswidget { *; }

# Required for Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Google Sign-In
-keep class com.google.android.gms.auth.api.signin.** { *; }
-dontwarn com.google.android.gms.auth.api.signin.**

# Keep model classes (adjust package name as needed)
-keep class com.sourav.livebusgietu.model.** { *; }

# Prevent obfuscation of classes that might be accessed by reflection
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod
-keepnames class * {
    @androidx.annotation.Keep *;
}
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# General Android support library keep rules
-keep class androidx.** { *; }
-dontwarn androidx.**

# Prevent errors related to reflection (optional)
-dontwarn sun.misc.**
# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**

# Google Sign-In
-keep class com.google.android.gms.auth.api.signin.** { *; }
-dontwarn com.google.android.gms.auth.api.signin.**

# Play Core
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

# Kotlin Lambda
-keepclassmembers class * {
    @kotlin.Metadata *;
}

-keep class kotlin.** { *; }
-dontwarn kotlin.**
