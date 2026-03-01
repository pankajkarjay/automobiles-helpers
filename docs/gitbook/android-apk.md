# Android APK Build Guide

## Prerequisites

- Android Studio (or Android SDK + command line tools)
- Supported JDK for your AGP version (typically JDK 17)

## Build

```bash
cd android-app
./gradlew assembleDebug
```

## Output

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install on phone

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
