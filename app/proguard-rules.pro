# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep data classes
-keep class com.mss.thebigcalendar.data.model.** { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }

# Keep Google API classes
-keep class com.google.api.** { *; }
-keep class com.google.android.gms.** { *; }

# Handle HTTP client conflicts
-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient
-keep class org.apache.http.** { *; }
-keep class android.net.http.AndroidHttpClient { *; }

# Keep Protobuf classes
-keep class com.google.protobuf.** { *; }

# Keep DataStore classes
-keep class androidx.datastore.** { *; }

# Remove ICU data (reduz ~25MB)
-dontwarn com.ibm.icu.**
-keep class com.ibm.icu.** { *; }

# Remove Apache Commons (reduz ~1.5MB)
-dontwarn org.apache.commons.**
-keep class org.apache.commons.** { *; }

# Remove JUnit from release (reduz ~0.01MB)
-dontwarn junit.**
-keep class junit.** { *; }

# Remove testing libraries from release
-dontwarn androidx.test.**
-keep class androidx.test.** { *; }

# Remove XR/OpenXR libraries (reduz ~5MB)
-dontwarn androidx.xr.**
-keep class androidx.xr.** { *; }

# Remove graphics path library (reduz ~0.01MB)
-dontwarn androidx.graphics.**
-keep class androidx.graphics.** { *; }

# Remove impress API (reduz ~20MB)
-dontwarn libimpress_api_jni.**
-keep class libimpress_api_jni.** { *; }

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile