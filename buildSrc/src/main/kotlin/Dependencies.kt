object Dependencies {

    // Kotlin
    const val kotlin_version = "1.9.20"
    const val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    const val kotlin_coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"

    // Android
    const val android_gradle_plugin = "com.android.tools.build:gradle:8.1.2"
    const val androidx_core = "androidx.core:core-ktx:1.12.0"
    const val androidx_appcompat = "androidx.appcompat:appcompat:1.6.1"
    const val androidx_lifecycle = "androidx.lifecycle:lifecycle-runtime-ktx:2.6.2"
    const val androidx_lifecycle_service = "androidx.lifecycle:lifecycle-service:2.6.2"
    const val androidx_activity = "androidx.activity:activity-ktx:1.8.0"
    const val androidx_fragment = "androidx.fragment:fragment-ktx:1.6.2"
    const val androidx_constraintlayout = "androidx.constraintlayout:constraintlayout:2.1.4"

    // AndroidX Permissions & Testing
    const val androidx_test_core = "androidx.test:core-ktx:1.5.0"
    const val androidx_test_runner = "androidx.test:runner:1.5.2"
    const val androidx_test_rules = "androidx.test:rules:1.5.0"
    const val androidx_test_espresso = "androidx.test.espresso:espresso-core:3.5.1"
    const val androidx_test_ext_junit = "androidx.test.ext:junit-ktx:1.1.5"
    const val androidx_test_uiautomator = "androidx.test.uiautomator:uiautomator:2.2.0"

    // Hilt Dependency Injection
    const val hilt_version = "2.48"
    const val hilt_android = "com.google.dagger:hilt-android:$hilt_version"
    const val hilt_compiler = "com.google.dagger:hilt-compiler:$hilt_version"
    const val hilt_testing = "com.google.dagger:hilt-android-testing:$hilt_version"
    const val hilt_android_gradle_plugin = "com.google.dagger:hilt-android-gradle-plugin:$hilt_version"

    // Testing
    const val junit = "junit:junit:4.13.2"
    const val mockk = "io.mockk:mockk:1.13.7"
    const val mockk_android = "io.mockk:mockk-android:1.13.7"
    const val robolectric = "org.robolectric:robolectric:4.11.1"
    const val truth = "com.google.truth:truth:1.1.5"

    // Network
    const val okhttp = "com.squareup.okhttp3:okhttp:4.11.0"
    const val retrofit = "com.squareup.retrofit2:retrofit:2.9.0"

    // Serialization
    const val moshi = "com.squareup.moshi:moshi-kotlin:1.15.0"
    const val gson = "com.google.code.gson:gson:2.10.1"

    // Logging
    const val timber = "com.jakewharton.timber:timber:5.0.1"

    // Material Design
    const val material = "com.google.android.material:material:1.10.0"

    // Crypto & Security
    const val androidx_security = "androidx.security:security-crypto:1.1.0-alpha06"
    const val bouncy_castle = "org.bouncycastle:bcprov-jdk15on:1.70"

    // Build time configurations
    const val minSdk = 34
    const val targetSdk = 34
    const val compileSdk = 34
    const val buildTools = "34.0.0"

    // NDK & Native
    const val ndk_version = "26.1.10909125"
}
