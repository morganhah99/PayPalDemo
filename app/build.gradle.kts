plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.paypaldemo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.paypaldemo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.analytics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.paypal.android:paypal-native-payments:1.4.0")

    implementation("com.paypal.android:payment-buttons:1.4.0")

    // Coroutines
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")


    //okhttp
    implementation ("com.squareup.okhttp3:okhttp:5.0.0-alpha.6")
    implementation ("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.6")

}
