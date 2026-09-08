# LINEA — Next-Generation Industrial Glassmorphic Android Dialer

[![Target Android 17](https://img.shields.io/badge/Android-17%20(API%2037)-5C7C99?style=flat&logo=android)](https://developer.android.com/about/versions/17)
[![Min SDK 30](https://img.shields.io/badge/Min%20SDK-30%20(Android%2011)-5C7C99?style=flat)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.11.00-4285F4?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Zero Cloud AI](https://img.shields.io/badge/Zero%20Cloud%20AI-100%25%20On--Device-success?style=flat)](https://github.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

LINEA (`com.ryanshelby.linea`) is a high-performance, lightweight, animated default cellular dialer replacement engineered for Android 17 (API 37). Built directly on Android’s native Telecom framework (`InCallService`, `ConnectionService`, `PhoneAccount`, and `CallScreeningService`), LINEA handles genuine cellular calls, DTMF signaling, multi-call conferencing, and carrier MMI features with zero cloud dependency.

---

## Key Highlights

- **Native Telecom Stack**: Full implementation of `InCallService` and `ConnectionService` registered as a system `PhoneAccount` for complete default dialer capabilities.
- **Zero Cloud or External AI**: 100% computed on-device. T9 prefix indexing, availability insights, pattern-based caller ID, repeat call overrides, and call screening rules run entirely within local Room SQLite queries and deterministic algorithms.
- **Industrial Glassmorphic Aesthetics**: Curated deep graphite palette (`#0D0F12` to `#1A1D21`), frosted glass surfaces (8% fill, 12% 1dp titanium borders), and tabular figures (`fontFeatureSettings = "tnum"`) across all timers, telemetry, and call metrics.
- **Multi-SIM & Carrier Control**: Per-contact SIM affinity rules, live signal dBm and technology telemetry, dual SIM selection dialogs with timed auto-call countdowns, and carrier MMI forwarding/barring commands.
- **Privacy & Security**: PIN-protected Private Contacts vault, offline country/region caller identification, on-device audio recording with amplitude waveform visualization, and full JSON data backup/migration.

---

## Design System

LINEA adheres strictly to an industrial-grade glassmorphic visual language:

| Design Token | Specification | Hex / Value |
|---|---|---|
| **Primary Background** | Dark Graphite Gradient | `#0D0F12` &rarr; `#1A1D21` |
| **Glass Panel Surface** | Semi-transparent dark fill | `Color.White.copy(alpha = 0.08f)` |
| **Glass Panel Border** | High-precision hairline stroke | `1.dp`, `Color.White.copy(alpha = 0.12f)` |
| **Primary Accent** | Titanium Blue | `#5C7C99` |
| **Success / Incoming** | Sage Green | `#5A8F6B` |
| **Danger / Blocked** | Rust Red / Brick Red | `#B5473F` / `#8C3B35` |
| **Warning / Pinned** | Amber Gold | `#D4A359` |
| **Numerical Figures** | Monospaced Tabular Figures | `FontFeature("tnum")` |

---

## Architectural Breakdown & Phase Roadmap

LINEA was engineered and verified across 8 sequential architectural phases:

### Phase 0: Foundation, Architecture & Design System
- Multi-layer clean architecture: Presentation (Compose), Domain, and Room Local Persistence.
- Room database (`LineaDatabase`) with 11 relational entities, foreign key cascades, and schema indices.
- Global glassmorphism primitives (`FrostedGlassBox`, `GlassPanel`, `FloatingGlassNavBar`).

### Phase 1: Core Calling Pipeline & Dialpad
- `LineaInCallService`: Manages system active calls, state callbacks, audio routes, and DTMF tones.
- `LineaConnectionService`: Registers `PhoneAccountHandle` with Telecom framework capabilities (`CAPABILITY_CALL_PROVIDER`).
- Fullscreen In-Call UI: Animated call duration counter, mute/hold/speaker toggles, in-call DTMF bottom sheet, and ambient caller glow.
- Dialpad with instantaneous T9 contact filtering (letters `2`–`9` matching contact names and numbers).

### Phase 2: Call History & Contacts Directory
- Coalescing call history: groups same-day repeated calls with expandable count badges (`×3`, `×6`).
- Interactive A-Z scrubber: vertical alphabet touch rail with haptic feedback ticks.
- Contact Detail & Create/Edit sheets with dual-write persistence to Android's `ContactsContract` and Room database.
- SIM affinity selector: forces individual contacts to dial via SIM 1 or SIM 2.

### Phase 3: Smart Call Management & Blocking Engine
- `CallScreeningEngine`: Real-time screening supporting exact numbers, wildcard prefixes (`*800*`), regex ranges, and silent ringing.
- Dual SIM Hub: Live carrier display, network type (`5G`, `LTE`), signal strength dBm bars, and custom SIM color badges.
- Auto-call countdown: 5-second cancelable dialog before dialing on designated default SIM slots.

### Phase 4: Advanced Calling, Recording & Voicemail
- In-call audio recorder: captures voice calls to local storage with amplitude metering and waveform playback.
- Call waiting and merge: handles concurrent calls, swapping active calls, and initiating 3-way conference calls.
- Visual Voicemail interface: playback slider, callback actions, and mark as read/unread.
- Carrier MMI code management: Call Forwarding (`*21*`, `*61*`) and Call Barring (`*33*`, `*35*`).

### Phase 5: Smart Rules Engine & DIM Mode
- Deterministic rules engine: Time-of-day quiet hours schedules, day-of-week recurrence, and emergency overrides.
- Repeat Call Tracker: Automatically bypasses quiet hours if the same number calls 3 times within 5 minutes.
- Pre-Call Notes: Pops contextual notes on the incoming call screen for designated clients or family members.
- Dark Industrial Minimalist (DIM) Mode: Ultra-low contrast night mode for OLED battery preservation.

### Phase 6: Differentiators, Insights & Privacy
- **Availability Insights**: Algorithmic 2-hour window answer rate analysis based on local call history.
- **Offline Caller ID**: Zero-network country/region detection, toll-free identification, and domestic carrier prefixes.
- **Private Contacts Vault**: 4-digit PIN-protected contacts hidden from main dialer tabs and system logs.
- **Call Diagnostics**: Real-time diagnostic tool detecting silent mode, DND status, airplane mode, and active block rules.
- **Backup & Migration**: Local JSON export and import for user rules, contacts, profiles, and settings.

### Phase 7: Polish, Edge Cases & Verification
- Comprehensive `BackHandler` navigation tree: sub-screens and sheets pop gracefully to parent tabs before exiting.
- Frosted glass empty states for search queries and zero-entry lists.
- Multi-hour call duration formatting (`1h 0m 0s`).
- 48 / 48 unit tests passing across all engines and database schemas.

---

## Tech Stack & Dependencies

- **Platform**: Android 17 (API 37), Min SDK 30
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (BOM 2024.11.00) + Material 3
- **Dependency Injection**: Hilt 2.52
- **Persistence**: Room 2.6.1 + Jetpack DataStore Preferences
- **Asynchronous**: Kotlin Coroutines & Flow
- **Background Work**: AndroidX WorkManager 2.10.0
- **Testing**: JUnit 4, Robolectric, Kotlinx Coroutines Test, Room In-Memory DB

---

## Building & Running

### Prerequisites
- JDK 17
- Android SDK Platform 37 & Build-Tools 35.0.0
- Android 17 (API 37) Emulator or physical device

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Build Debug APK
```bash
./gradlew assembleDebug
# Binary created at: app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK
```bash
./gradlew assembleRelease
# Binary created at: app/build/outputs/apk/release/app-release-unsigned.apk
```

### Install to Connected Device
```bash
./gradlew installDebug
adb shell am start -n com.ryanshelby.linea/.MainActivity
```

---

## Default Dialer Setup

To set LINEA as the default dialer on your device:
1. Open LINEA and tap the **Settings** tab.
2. Select **Permissions & System Integration**.
3. Tap **Set as Default Dialer** to trigger the system default dialer prompt.
4. Alternatively, execute via ADB:
```bash
adb shell telecom set-default-dialer com.ryanshelby.linea
```
