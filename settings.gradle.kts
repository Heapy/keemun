plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

gradle.beforeProject {
    extensions.extraProperties["konan.data.dir"] = rootDir.resolve(".gradle/konan").absolutePath
}

rootProject.name = "keemun"
include("app")
