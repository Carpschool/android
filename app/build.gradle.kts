import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val local = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun prop(k: String, d: String = "") = (local.getProperty(k) ?: System.getenv(k) ?: d)

android {
    namespace = "ca.carpschool"
    compileSdk = 37
    defaultConfig {
        applicationId = "ca.carpschool"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"
        manifestPlaceholders["MAPS_API_KEY"] = prop("MAPS_API_KEY")
        buildConfigField("String", "MAPS_API_KEY", "\"${prop("MAPS_API_KEY")}\"")
        buildConfigField("String", "CLERK_PUBLISHABLE_KEY", "\"${prop("CLERK_PUBLISHABLE_KEY")}\"")
        buildConfigField("String", "CENTRAL_URL", "\"${prop("CENTRAL_URL")}\"")
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES") }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("com.clerk:clerk-android-ui:1.1.11")
    implementation("com.google.maps.android:maps-compose:9.0.0")
    implementation("com.google.android.libraries.places:places:6.0.2")
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("io.socket:socket.io-client:2.1.2") { exclude(group = "org.json", module = "json") }
    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")
}
