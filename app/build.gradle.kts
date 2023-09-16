plugins {
  id("com.android.application")
  id("com.google.gms.google-services")
  kotlin("android")
  kotlin("kapt")
}

android {
  namespace = "com.invorel.blankchatpro"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.invorel.blankchatpro"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
  composeOptions {
    kotlinCompilerExtensionVersion = "1.4.3"
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {

  //Production
  implementation("androidx.core:core-ktx:1.10.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
  implementation("androidx.activity:activity-compose:1.7.2")
  implementation(platform("androidx.compose:compose-bom:2023.03.00"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3:1.2.0-alpha06")
  implementation ("androidx.navigation:navigation-compose:2.7.1")
  //DataStore
  implementation("androidx.datastore:datastore-preferences:1.0.0")
  //Firebase
  implementation(platform("com.google.firebase:firebase-bom:32.2.3"))
  implementation("com.google.firebase:firebase-auth-ktx")
  implementation("com.google.firebase:firebase-storage-ktx")
  implementation("com.google.firebase:firebase-firestore-ktx")
  implementation("com.google.firebase:firebase-messaging-ktx")
  //Coil to load image from Uri
  implementation("io.coil-kt:coil-compose:2.4.0")
  //Room
  implementation("androidx.room:room-runtime:2.5.2")
  kapt("androidx.room:room-compiler:2.5.2")
  //Gson
  implementation("com.google.code.gson:gson:2.8.8")

  //Testing
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
  androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")

  //Debug
  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")

}