// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.android)  apply false
    alias(libs.plugins.ksp) // Use the alias you defined in libs.versions.toml
}
