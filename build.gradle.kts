// Build Script do PROJETO (Raiz)
plugins {
    // Aplica as versões novas definidas no libs.versions.toml
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
}