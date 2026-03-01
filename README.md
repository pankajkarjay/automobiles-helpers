# Secure Vehicle Companion (Authorized Demo)

This repository now contains **two deliverables**:

1. A **Web PWA** demo for authorized Bluetooth vehicle control.
2. A native **Android app project** (`android-app/`) that can be built into an APK in a standard Android toolchain environment.

## Safety and legal notice

- Only use with vehicles you own or are explicitly authorized to control.
- This project does **not** bypass immobilizers/OEM security.
- This project does **not** support overriding nearby vehicle signals.
- Production deployments must use OEM-approved APIs, strong cryptography, and legal compliance.

## Web app (PWA)

### Run locally

```bash
python3 -m http.server 8080
```

Open `http://localhost:8080` in a Chromium-based browser.

### Bluetooth placeholders

Update these in `app.js`:

- `VEHICLE_SERVICE_UUID`
- `VEHICLE_COMMAND_CHARACTERISTIC_UUID`

Supported command payloads: `LOCK`, `UNLOCK`, `START`, `STOP`.

## Android APK project

Android project path: `android-app/`

### What is included

- Native BLE scan/connect/disconnect flow.
- Action commands: lock/unlock/start/stop.
- Runtime permission handling (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, fallback location on older Android).
- Simple UI with status and log.

### Build debug APK

From repository root:

```bash
cd android-app
./gradlew assembleDebug
```

Expected output APK path:

```text
android-app/app/build/outputs/apk/debug/app-debug.apk
```

> If your environment uses an unsupported or very new Java runtime for Android Gradle Plugin, switch to a supported JDK (commonly 17) before building.

## GitBook publishing content

A GitBook-ready docs set has been prepared at:

- `docs/gitbook/README.md`
- `docs/gitbook/android-apk.md`
- `docs/gitbook/security.md`
- `docs/gitbook/SUMMARY.md`

You can import/publish these files in your GitBook space.
