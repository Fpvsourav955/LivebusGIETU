plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.sourav.livebusgietu"
    compileSdk = 36


    defaultConfig {
        applicationId = "com.sourav.livebusgietu"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {

        release {

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }


    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures{
        dataBinding=true
        viewBinding = true

    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation(libs.circleimageview)
    implementation(libs.viewpager2)
    implementation(libs.lottie)
    implementation ("com.github.bumptech.glide:glide:5.0.5")
    implementation(libs.firebase.inappmessaging.display)
    annotationProcessor (libs.compiler)
    implementation(libs.roundedimageview)
    implementation(libs.play.services.auth)
    implementation("com.google.firebase:firebase-appcheck-debug:19.0.1")
    implementation("com.github.ybq:Android-SpinKit:1.4.0")
    implementation("io.github.chaosleung:pinview:1.4.4")
    implementation("com.google.firebase:firebase-appcheck-playintegrity:19.0.1")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.20")
    implementation ("com.github.Foysalofficial:NafisBottomNav:5.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.play.services.auth)
    implementation("com.google.android.play:app-update:2.1.0")
    implementation(libs.firebase.messaging)
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")


    implementation(libs.otpview)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}