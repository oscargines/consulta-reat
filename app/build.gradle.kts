import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.oscar.consultareat"
    compileSdk {
        version = release(37)
    }

    signingConfigs {
        val keystore = rootProject.file("release.keystore")
        val storePwd = providers.gradleProperty("RELEASE_STORE_PASSWORD").orNull
        val keyPwd = providers.gradleProperty("RELEASE_KEY_PASSWORD").orNull
        if (keystore.exists() && storePwd != null && keyPwd != null) {
            create("release") {
                storeFile = keystore
                storePassword = storePwd
                keyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").orNull ?: "consulta-reat-key"
                keyPassword = keyPwd
            }
        }
    }

    defaultConfig {
        applicationId = "com.oscar.consultareat"
        minSdk = 31
        targetSdk = 37
        versionCode = 4
        versionName = "1.2.0"

        val localProps = rootProject.file("local.properties")
        val napApiKey = if (localProps.exists()) {
            val props = Properties()
            localProps.inputStream().use { props.load(it) }
            props.getProperty("NAP_API_KEY", "")
        } else {
            ""
        }
        buildConfigField("String", "NAP_API_KEY", "\"${napApiKey.replace("\"", "\\\"")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            optimization {
                enable = false
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
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.icons.core)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.camera.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.okhttp.logging)
    implementation(libs.jsoup)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(fileTree("libs") { include("*.jar") })
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
