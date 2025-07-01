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
        testInstrumentationRunnerArguments["IS_TEST"] = "true"
        testInstrumentationRunnerArguments["timeout_msec"] = "120000"
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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
    implementation(libs.monitor)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    //noinspection GradleDependency
// Чтобы запускать Android-тесты на эмуляторе
    androidTestImplementation(libs.runner)
    androidTestImplementation("androidx.test:rules:1.6.1")

// Библиотеки для самих тестов
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core.v361)
    implementation(libs.okhttp)

}