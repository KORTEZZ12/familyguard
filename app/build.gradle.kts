plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("jacoco")
}


android {
    namespace = "com.example.testjava"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.testjava"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    //noinspection GradleDependency
    implementation(libs.work.runtime)
    implementation(libs.okhttp)
    androidTestImplementation(libs.rules)
    androidTestImplementation(libs.espresso.core.v361)
}