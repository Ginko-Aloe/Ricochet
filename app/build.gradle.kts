import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.ginkoaloe.ricochet"

    // Declare a Android les langues de l'application (values/, values-fr/...) : le
    // telephone propose alors de choisir la langue de Ricochet dans ses reglages.
    androidResources {
        generateLocaleConfig = true
    }
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.ginkoaloe.ricochet"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // La cle de signature ne vit jamais dans le depot. La propriete Gradle
    // « ricochet.signature », posee dans ~/.gradle/gradle.properties, donne le chemin
    // d'un fichier .properties (storeFile, keyAlias) ; le mot de passe vient de la
    // variable d'environnement RICOCHET_MOT_DE_PASSE. Sans eux, l'APK de version sort
    // non signe : c'est ce que construit F-Droid avant de comparer avec le notre.
    val signature = providers.gradleProperty("ricochet.signature").orNull?.let(::file)
    val motDePasse = providers.environmentVariable("RICOCHET_MOT_DE_PASSE").orNull
    signingConfigs {
        if (signature != null && motDePasse != null) {
            create("publication") {
                val reglages = Properties().apply { signature.inputStream().use(::load) }
                storeFile = file(reglages.getProperty("storeFile"))
                keyAlias = reglages.getProperty("keyAlias")
                storePassword = motDePasse
                keyPassword = motDePasse
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("publication")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }

    // Le bloc de dependances qu'AGP glisse par defaut dans l'APK est chiffre avec une
    // cle de Google : illisible pour quiconque d'autre, F-Droid le refuse.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}