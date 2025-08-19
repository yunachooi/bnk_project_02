plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.example.bnk_project_02f"
    //compileSdk = flutter.compileSdkVersion
    //ndkVersion = flutter.ndkVersion
    // 필요 시 아래 한 줄만 사용하거나 위의 flutter.ndkVersion만 사용하세요.
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.example.bnk_project_02f"
        // desugaring 요구 사항: minSdk는 21 이상 권장
        minSdk = 21
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    // ★ Java 17 + core library desugaring 활성화
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            // 데모 용도: debug 키로 서명
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

flutter {
    source = "../.."
}

dependencies {
    // ★ desugaring 라이브러리 추가
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    // 다른 의존성들은 Flutter가 자동으로 주입합니다.
}
