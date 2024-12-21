package com.example.util.simpletimetracker

object BuildPlugins {
    const val gradle = "com.android.tools.build:gradle:${Versions.gradle}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val ktlint = "org.jlleitschuh.gradle:ktlint-gradle:${Versions.ktlint}"
    const val hilt = "com.google.dagger:hilt-android-gradle-plugin:${Versions.dagger}"

    const val ksp = "com.google.devtools.ksp"
}