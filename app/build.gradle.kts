plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "ir.dbsgraphic.secondbrain"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.dbsgraphic.secondbrain"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // RTL is the default layout direction — Persian is the host language.
        resourceConfigurations += listOf("fa", "en")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:security"))
    implementation(project(":core:ai"))
    implementation(project(":feature:inbox"))
    implementation(project(":feature:itemdetail"))
    implementation(project(":feature:project"))
    implementation(project(":feature:timeline"))
    implementation(project(":feature:search"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
