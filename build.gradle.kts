// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath(com.example.util.simpletimetracker.BuildPlugins.gradle)
        classpath(com.example.util.simpletimetracker.BuildPlugins.kotlin)
        classpath(com.example.util.simpletimetracker.BuildPlugins.ktlint)
        classpath(com.example.util.simpletimetracker.BuildPlugins.hilt)

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

plugins {
    id(com.example.util.simpletimetracker.BuildPlugins.ksp)
        .version(com.example.util.simpletimetracker.Versions.ksp)
        .apply(false)
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

tasks {
    val clean by registering(Delete::class) {
        delete(buildDir)
    }
}
