import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  val verCode = (project.findProperty("VERSION_CODE") as? String)?.toIntOrNull() ?: 1
  val defaultVerName = SimpleDateFormat("yyyy.MM.dd", Locale.US).format(Date())
  val verName = (project.findProperty("VERSION_NAME") as? String) ?: defaultVerName

  defaultConfig {
    applicationId = "app.fieldnotes"
    minSdk = 26
    targetSdk = 36
    versionCode = verCode
    versionName = verName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePropsFile = rootProject.file("keystore.properties")
      val keystoreProps = Properties()
      if (keystorePropsFile.exists()) {
        keystoreProps.load(keystorePropsFile.inputStream())
      }

      val envStorePath = System.getenv("KEYSTORE_PATH")
      val localStorePath = keystoreProps.getProperty("storeFile")

      val resolvedStorePath = when {
        !envStorePath.isNullOrBlank() && file(envStorePath).exists() -> envStorePath
        !localStorePath.isNullOrBlank() && file(localStorePath).exists() -> localStorePath
        file("${rootDir}/keystore/release.jks").exists() -> "${rootDir}/keystore/release.jks"
        file("${rootDir}/my-upload-key.jks").exists() -> "${rootDir}/my-upload-key.jks"
        file("${rootDir}/release.jks").exists() -> "${rootDir}/release.jks"
        else -> "${rootDir}/keystore/release.jks"
      }

      storeFile = file(resolvedStorePath)
      storePassword = System.getenv("STORE_PASSWORD")
        ?: keystoreProps.getProperty("storePassword")
        ?: "fieldnotes123"
      keyAlias = System.getenv("KEY_ALIAS")
        ?: keystoreProps.getProperty("keyAlias")
        ?: "fieldnotes"
      keyPassword = System.getenv("KEY_PASSWORD")
        ?: keystoreProps.getProperty("keyPassword")
        ?: "fieldnotes123"
    }
    create("debugConfig") {
      val localDebugFile = when {
        file("${rootDir}/debug-upload.keystore").exists() -> file("${rootDir}/debug-upload.keystore")
        file("${rootDir}/debug.keystore").exists() -> file("${rootDir}/debug.keystore")
        else -> file("${rootDir}/debug-upload.keystore")
      }
      storeFile = localDebugFile
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      if (file("${rootDir}/debug.keystore").exists() || file("${rootDir}/debug-upload.keystore").exists()) {
        signingConfig = signingConfigs.getByName("debugConfig")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
