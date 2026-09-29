# 🚀 LiNEA v2.4.1 — Release Notes

## Overview
**LiNEA v2.4.1** is a critical patch release fixing the QR code scanner crash on physical devices, adding reliable hardware button call controls (volume/power silencing and power double-press to end call), and resolving 30+ application-wide bugs across telecom stability, security, and UI/UX.

---

### What's Fixed in v2.4.1

#### 📸 QR Code Scanner — Physical Device Camera Crash & Blank Screen
- **R8 / ProGuard Fix**: Added `-keep` rules for Google ML Kit component discovery, CameraX, and ZXing classes in `proguard-rules.pro`. Release builds with R8 minification were stripping ML Kit reflection, causing a `NullPointerException` crash when opening the QR scanner on physical devices.
- **Real-Time Offline ZXing Fallback**: If ML Kit fails to initialize (stripped by R8, missing Google Play Services, or device-specific issues), the scanner seamlessly falls back to offline ZXing Y-plane luminance decoding — zero external service dependencies.
- **OEM Lifecycle Fix**: Resolved blank camera frames on Samsung OneUI, Xiaomi HyperOS, and ColorOS caused by synthetic dialog window lifecycles by resolving the hosting `ComponentActivity` directly.
- **3-Tier Camera Resolution Strategy**: Automatic fallback from 720p → CDD default → Preview-only to prevent `IllegalArgumentException` crashes across all camera hardware levels (`LEGACY`, `LIMITED`, `FULL`).
- **Interactive Error UI**: Camera failures now show **"Retry Camera"** and **"Scan Photo"** buttons instead of a blank screen.

#### 📞 Incoming Call Hardware Button Behavior
- **Volume Up/Down → Silence**: Pressing either volume button during an incoming call immediately silences the ringtone. Grace period reduced from 2000ms to 600ms for near-instant responsiveness.
- **Power Button → Silence**: A single power button press reliably silences the ringtone. Removed the proximity sensor gate that was blocking power button behavior when the phone was near the user's face or in a pocket.
- **Power Button ×2 → End Call**: Two rapid power button presses (within 1 second) reject/terminate the call — works for incoming, outgoing, and active calls. Implemented via `ACTION_SCREEN_OFF` + `ACTION_SCREEN_ON` broadcast detection.

#### 🔧 Full Application Audit & Multi-Phase Bug Fixes
- **Telecom & In-Call**: Fixed double-dispose crash on rejected calls, audio route desync, SIM selection race condition, duration timer drift, and missing `FOREGROUND_SERVICE_PHONE_CALL` on Android 14+.
- **Security & Vault**: Hardened biometric re-authentication, constant-time PIN comparison, and orphaned contact cleanup on deletion.
- **UI/UX**: Eliminated scroll jank on Recent calls, fixed DTMF overlap, search highlight desync, corrupt backup/restore handling, and light theme contrast issues.

### What's New in v2.4.1
- Closed-app NFC mutual beam processing, tag detection diagnostics, and retry workflows.
- "Always Share" mode and persistent digital card profile synchronization.
- Dynamic app version display in Settings footer.

> **⚠️ NFC Beaming is currently in the testing phase.** If you encounter any bugs or issues with NFC contact sharing, please report them to [ryn@disr.it](mailto:ryn@disr.it).

---

### Release Metadata
- **versionCode**: `7`
- **versionName**: `2.4.1`
- **Min SDK**: `Android 11 (API 30)`
- **Target SDK**: `Android 17 (API 37)`
