import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion: String by System.getProperties()

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("org.jetbrains.compose") version "1.2.0-alpha01-dev770"
}

group = "alex.exe.simmi"
version = "2.0.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

tasks.processResources {
    filesMatching("buildinfo.properties") {
        expand(project.properties)
    }
}

dependencies {
    val ktorVersion = "2.2.1"

    implementation(compose.desktop.currentOs)

    implementation("org.slf4j:slf4j-simple:2.0.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("com.github.kwhat:jnativehook:2.2.2")
    implementation("com.github.twitch4j:twitch4j:1.12.0")
    implementation("com.github.tkuenneth:nativeparameterstoreaccess:0.1.2")

    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-server-partial-content:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response:$ktorVersion")

    implementation("dev.kord:kord-core:0.8.0-M16")
    implementation("org.jsoup:jsoup:1.15.3")

    implementation("com.google.api-client:google-api-client:2.1.1")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20221216-2.0.0")

    implementation("org.apache.commons:commons-text:1.10.0")
}


tasks.withType<KotlinCompile> {
    kotlin.sourceSets.all {
        languageSettings.apply {
            optIn("kotlin.RequiresOptIn")
            optIn("kotlin.time.ExperimentalTime")
            optIn("androidx.compose.ui.ExperimentalComposeUiApi")
        }
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Exe_Simmi"
            packageVersion = version.toString()
        }
    }
}