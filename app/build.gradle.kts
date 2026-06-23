plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    listOf(
        linuxX64(),
        linuxArm64(),
        macosArm64(),
    ).forEach { target ->
        target.binaries {
            executable {
                baseName = "keemun"
                entryPoint = "io.heapy.keemun.main"
            }
        }
    }

    jvmToolchain(25)

    sourceSets {
        commonMain {
            kotlin.srcDir("src/main/kotlin")
            dependencies {
                implementation(libs.clikt)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.okio)
            }
        }

        jvmMain {
            dependencies {
                runtimeOnly(libs.logback.classic)
            }
        }

        jvmTest {
            kotlin.srcDir("src/test/kotlin")
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit.jupiter.api)
                runtimeOnly(libs.junit.jupiter.engine)
                implementation(libs.ktor.server.test.host)
            }
        }
    }
}

tasks.register<JavaExec>("run") {
    group = "application"
    description = "Runs this project as a JVM application"
    dependsOn("jvmMainClasses")
    mainClass = "io.heapy.keemun.MainKt"
    classpath = files(
        kotlin.targets["jvm"].compilations["main"].output.allOutputs,
        kotlin.targets["jvm"].compilations["main"].runtimeDependencyFiles,
    )
    workingDir = rootProject.projectDir
}

val nativeReleaseSource = layout.buildDirectory.file("bin/macosArm64/releaseExecutable/keemun.kexe")
val nativeReleaseDestinationDir = layout.buildDirectory.dir("native")

tasks.register<Copy>("nativeReleaseBinary") {
    group = "build"
    description = "Builds a macOS ARM64 Kotlin/Native release executable at build/native/keemun"
    dependsOn("linkReleaseExecutableMacosArm64")
    from(nativeReleaseSource) {
        rename { "keemun" }
    }
    into(nativeReleaseDestinationDir)
}

tasks.named("assemble") {
    dependsOn("nativeReleaseBinary")
}
