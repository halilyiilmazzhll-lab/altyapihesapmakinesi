import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.egimhesabi"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.hll.egimhesabi"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "2.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val signing = Properties()
            val privateFile = File(System.getProperty("user.home"), ".gradle/egimhesabi-signing/signing.properties")
            if (privateFile.isFile) privateFile.reader(Charsets.UTF_8).use { signing.load(it) }
            fun setting(name: String, environment: String): String? =
                providers.environmentVariable(environment).orNull ?: signing.getProperty(name)
            setting("storeFile", "EGIM_KEYSTORE_FILE")?.let { storeFile = file(it) }
            storePassword = setting("storePassword", "EGIM_KEYSTORE_PASSWORD")
            keyAlias = setting("keyAlias", "EGIM_KEY_ALIAS")
            keyPassword = setting("keyPassword", "EGIM_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation("androidx.core:core-splashscreen:1.0.1")
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation("androidx.compose.material:material-icons-extended")

  // DataStore & Serialization
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.serialization.json)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.core)

  // Navigation 3
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Haze for Frosted Glass
  implementation(libs.haze)
  implementation(libs.haze.materials)

  // Room
  val room_version = "2.7.0-alpha13"
  implementation("androidx.room:room-runtime:$room_version")
  implementation("androidx.room:room-ktx:$room_version")
  ksp("androidx.room:room-compiler:$room_version")

  // FastExcel for .xlsx generation
  implementation("org.dhatim:fastexcel:0.12.11")
  // Legacy Excel 97–2003 (.xls); OOXML imports use bounded SAX/ZIP parsing.
  implementation("net.sourceforge.jexcelapi:jxl:2.6.12")
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}




tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xencoding=UTF-8")
    }
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

