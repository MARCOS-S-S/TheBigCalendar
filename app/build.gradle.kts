import java.util.Properties

// Caminho: app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // CORREÇÃO: Este plugin é agora OBRIGATÓRIO com as versões recentes do Kotlin/Compose.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.protobuf)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.mss.thebigcalendar"
    compileSdk = 35

    sourceSets {
        getByName("main") {
            java.srcDirs("build/generated/source/proto/main/java")
        }
    }

    defaultConfig {
        applicationId = "com.mss.thebigcalendar"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Adicione este bloco para carregar as propriedades da sua keystore
    val keystorePropertiesFile = rootProject.file("app/keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Adicione esta linha para usar a configuração de assinatura
            signingConfig = signingConfigs.getByName("release")
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
        // Esta linha continua sendo necessária para o Android Gradle Plugin
        compose = true
    }

    // CORREÇÃO: Este bloco foi removido, pois o novo plugin gerencia isso.
    // composeOptions {
    //     kotlinCompilerExtensionVersion = "1.5.10"
    // }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,DEPENDENCIES}"
        }
    }
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Dependências de ViewModel e Ícones
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.compose.material:material-icons-extended")

    // Dependências do DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)

    // Dependência do Protobuf para o Proto DataStore
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.places)

    // Dependências para o Login com Google e API do Calendar
    implementation(libs.google.auth)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.calendar)
    
    // WorkManager para sincronização em background
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Biblioteca para gráficos
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Dependência do Gson para leitura do arquivo JSON
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.androidx.tools.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)

    // Dependências de Teste
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}