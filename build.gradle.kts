import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    alias(libs.plugins.compose.compiler)
}

group = "org.tahomarobotics.scouting"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://jitpack.io")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")
    implementation("com.github.betterbearmetalcode:koala:c654872")
    implementation("ch.qos.logback:logback-classic:1.5.15")
    implementation("org.dhatim:fastexcel:0.18.4")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.mongodb:mongodb-driver-sync:5.2.1")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.AppImage)
            packageName = "scouting-server"
            packageVersion = "1.0.0"
        }
        buildTypes.release {

        }
        buildTypes.release.proguard {
            isEnabled = false
        }
    }
}