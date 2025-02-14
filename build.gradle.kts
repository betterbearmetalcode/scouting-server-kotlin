import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "org.tahomarobotics.scouting"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
//    maven {
//        url = uri("https://maven.pkg.github.com/betterbearmetalcode/koala")
//        credentials {
//            username = System.getenv("GITHUB_USERNAME")
//            password = System.getenv("GITHUB_TOKEN")
//        }
//    }

    maven("https://jitpack.io")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")
    implementation("com.github.betterbearmetalcode:koala:v1.0.2")
    implementation("ch.qos.logback:logback-classic:1.5.15")
}


compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "scouting-server"
            packageVersion = "1.0.0"
        }
    }
}
