plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt") // Agregar este plugin para Room

}

android {
    namespace = "com.dapm.weatherfit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dapm.weatherfit"
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
}

dependencies {

    //Material 3
    implementation ("com.google.android.material:material:1.11.0")

    //viewpage
    implementation ("androidx.viewpager2:viewpager2:1.0.0")

    //apiclima
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    //api gemini
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")

    // Room dependencies
    implementation ("androidx.room:room-runtime:2.6.1")
    implementation(libs.androidx.swiperefreshlayout)
    kapt ("androidx.room:room-compiler:2.6.1")
    // Para corrutinas
    implementation ("androidx.room:room-ktx:2.6.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}