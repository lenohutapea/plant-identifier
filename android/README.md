# Plant Identifier — Android app

Kotlin + Jetpack Compose client for the Plant Identifier API.

- Take a photo with the camera app or pick one from the gallery (no camera permission needed).
- The photo is resized to at most 1024 px and re-encoded as JPEG before upload.
- Shows the plant, leaf condition (with the API's `condition_status`), top-3 predictions and
  Indonesian care information from `GET /plants/{id}`.
- Every diagnosis is stored in Room (`diagnoses` table) together with the full API responses,
  so history entries open offline.

Architecture: MVVM — `MainViewModel` → `DiagnosisRepository` → Retrofit (`PlantApi`) + Room (`DiagnosisDao`),
wired by a small manual container (`di/AppContainer`).

## API address

Set in `gradle.properties` (`apiBaseUrl`), default `http://plant-api.10.212.51.11.nip.io/`.
Override for one build: `./gradlew assembleDebug -PapiBaseUrl=http://10.0.2.2:8000/` (emulator → host machine).
Cleartext HTTP is allowed only for the hosts in `res/xml/network_security_config.xml`.

## Build

CI builds the app on every push (`.github/workflows/android.yml`) with Gradle 8.11.1 and JDK 17, runs the
unit tests and publishes the debug APK to the
[android-latest pre-release](https://github.com/lenohutapea/plant-identifier/releases/tag/android-latest).

Locally: open `android/` in Android Studio, or with Gradle 8.11.1 installed run
`gradle testDebugUnitTest assembleDebug` (APK in `app/build/outputs/apk/debug/`).
