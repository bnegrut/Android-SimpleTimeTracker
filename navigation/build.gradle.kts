import com.example.util.simpletimetracker.Base
import com.example.util.simpletimetracker.Deps
import com.example.util.simpletimetracker.applyAndroidLibrary

plugins {
    id(com.example.util.simpletimetracker.BuildPlugins.gradleLibrary)
    id(com.example.util.simpletimetracker.BuildPlugins.kotlin)
    id(com.example.util.simpletimetracker.BuildPlugins.kotlinParcelize)
    id(com.example.util.simpletimetracker.BuildPlugins.ksp)
}

applyAndroidLibrary()

android {
    namespace = "${Base.namespace}.navigation"
}

dependencies {
    implementation(project(":domain"))

    implementation(Deps.Ktx.fragment)
    implementation(Deps.Ktx.navigationFragment)
    implementation(Deps.Ktx.navigationUi)
}
