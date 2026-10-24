plugins { id("com.android.application") }

android {
    namespace = "com.emeka45.universalvideodownloader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.emeka45.universalvideodownloader"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<Test>().configureEach { useJUnit() }

dependencies {
    testImplementation("junit:junit:4.13.2")
}
