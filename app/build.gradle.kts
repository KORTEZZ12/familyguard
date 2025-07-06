plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("io.gitlab.arturbosch.detekt") version "1.23.0"
    id("pmd")
    id("com.github.spotbugs") version "6.0.26"
}

android {
    namespace = "com.example.testjava"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.familyguard"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.activity:activity:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2") {
        exclude(group = "org.hamcrest", module = "hamcrest-core")
    }
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

detekt {
    config.from("$projectDir/config/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
}

pmd {
    ruleSets = listOf("basic", "braces", "clone", "codesize", "design", "empty", "finalizers", "imports", "naming", "optimizations", "strictexception", "strings", "unusedcode")
    ruleSetFiles = files("$projectDir/config/pmd-ruleset.xml")
    sourceSets = sourceSets
}

tasks.register<org.gradle.api.plugins.quality.Pmd>("pmd") {
    group = "verification"
    description = "Run PMD analysis"
    source("src/main/java")
    include("**/*.java")
    ruleSetFiles = files("$projectDir/config/pmd-ruleset.xml")
    ignoreFailures = false
}

spotbugs {
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.HIGH)
    excludeFilter.set(file("$projectDir/spotbugs-exclude.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports {
        xml {
            required.set(true)
            setDestination(file("${layout.buildDirectory.get().asFile}/reports/spotbugs/spotbugs.xml"))
        }
        html {
            required.set(true)
            setDestination(file("${layout.buildDirectory.get().asFile}/reports/spotbugs/spotbugs.html"))
        }
    }
}