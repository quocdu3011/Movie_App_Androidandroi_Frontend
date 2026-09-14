plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.movieapp.core.database"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core-common"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    implementation(libs.hilt.android)

    testImplementation(libs.junit)
}
