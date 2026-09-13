# Foreground Service Example

Minimal Android foreground service, built from the CLI without Android
Studio

## Build & Run

```
make build    # assemble debug APK
make install  # build + adb install
make emulator # start AVD (Pixel_API_35 by default)
make clean    # remove build artifacts
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Requirements

- JDK 17
- Android SDK with `platforms;android-34`, `build-tools;34.0.0`,
  `platform-tools`
- SDK path must be known to Gradle — via `ANDROID_HOME` env var, or
  `local.properties` with `sdk.dir=/path/to/Android/Sdk`

## Makefile

Change `AVD_NAME` if your emulator is named differently

## Note on `foregroundServiceType`

Declared as `dataSync` just because Android 14+ requires some type.
Semantically wrong — no sync happens
