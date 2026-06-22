plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    runtimeOnly(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}

testing {
    suites {
        // Configure the built-in test suite
        val test = named<JvmTestSuite>("test") {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("6.0.1")
        }
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    // Define the main class for the application.
    mainClass = "io.heapy.keemun.MainKt"
    applicationName = "keemun"
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
