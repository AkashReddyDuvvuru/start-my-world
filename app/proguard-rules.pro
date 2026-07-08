# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep our code
-keep class com.stealthstream.** { *; }
-keep interface com.stealthstream.** { *; }

# Hilt
-keep class dagger.hilt.internal.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keepclasseswithmembernames class * {
    @dagger.hilt.** *;
}

# Native JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# AndroidX
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Timber
-dontwarn timber.log.Timber

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Moshi
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}

# Kotlin
-keep class kotlin.** { *; }
-dontwarn kotlin.**
-keepclassmembers class * extends java.lang.Enum {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
