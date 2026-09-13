plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    compileOnly(files("/home/serr/software/android-sdk/platforms/android-34/android.jar"))
    implementation("org.json:json:20231013")
}

kotlin {
    jvmToolchain(17)
}
