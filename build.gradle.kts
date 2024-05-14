plugins {
    idea
    alias(libs.plugins.kotlin)
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

allprojects {
    apply(plugin = rootProject.libs.plugins.kotlin.get().pluginId)

    val version: String by project

    this.version = version
    this.group = "xyz.icetang.lib"

    repositories {
        mavenCentral()
    }
}

subprojects {
    repositories {
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))

        compileOnly(rootProject.libs.paper)
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "21"
}