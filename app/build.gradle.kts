plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "lucns.whatsapp.fastmessage"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "lucns.whatsapp.fastmessage"
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
}