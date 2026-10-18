import groovy.json.JsonSlurper
import java.util.Properties
import java.security.KeyStore
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
val appVersion = Properties().apply { rootProject.file("version.properties").inputStream().use { load(it) } }
val suppliedName = providers.gradleProperty("releaseVersionName").orNull
val suppliedCode = providers.gradleProperty("releaseVersionCode").orNull
require((suppliedName == null) == (suppliedCode == null)) { "Supply both releaseVersionName and releaseVersionCode." }
val resolvedName = suppliedName ?: appVersion.getProperty("versionName")
val resolvedCode = (suppliedCode ?: appVersion.getProperty("versionCode")).toInt()
require(Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)").matches(resolvedName)) { "Use MAJOR.MINOR.PATCH." }
require(resolvedCode in 1..2100000000) { "Invalid Android versionCode." }
val releaseHistory = JsonSlurper().parse(rootProject.file("releases/history.json")) as Map<*, *>
val pastReleases = (releaseHistory["releases"] as List<*>).map { it as Map<*, *> }
val highestRelease = pastReleases.maxBy { (it["versionCode"] as Number).toInt() }
val highestCode = (highestRelease["versionCode"] as Number).toInt()
require(resolvedCode > highestCode || (resolvedCode == highestCode && resolvedName == highestRelease["versionName"])) {
    "Version metadata would regress or relabel a known release. Use tools/release.py."
}

android {
    namespace = "com.piccol.hifi"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.piccol.hifi"
        minSdk = 26
        targetSdk = 36
        versionCode = resolvedCode
        versionName = resolvedName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes {
        release { isMinifyEnabled = false; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }
    }
    lint { abortOnError = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.01"))
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("androidx.room:room-paging:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.paging:paging-compose:3.3.6")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.16")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}

// Never let AGP silently create a replacement key for the existing installable APK line.
android.signingConfigs.getByName("debug").apply {
    providers.environmentVariable("HIFI_DEBUG_KEYSTORE").orNull?.let { storeFile = file(it) }
}
tasks.matching { it.name.startsWith("validateSigningDebug") }.configureEach {
    outputs.upToDateWhen { false }
    doFirst {
        val signing = android.signingConfigs.getByName("debug")
        val keyFile = signing.storeFile ?: error("Restore the existing update keystore.")
        check(keyFile.isFile) { "Existing update keystore missing: $keyFile. Restore it; do not generate a new certificate." }
        val store = KeyStore.getInstance(keyFile, signing.storePassword!!.toCharArray())
        val digest = MessageDigest.getInstance("SHA-256").digest(store.getCertificate(signing.keyAlias).encoded)
            .joinToString("") { "%02x".format(it) }
        check(digest == releaseHistory["updateCertificateSha256"]) {
            "Signing certificate changed. This APK cannot update existing installs. Restore the original key."
        }
    }
}
