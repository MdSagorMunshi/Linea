<p align="center">
  <img src="docs/assets/screenshots/about_foss.png" alt="LINEA Dialer Banner" width="180" style="border-radius: 28px; box-shadow: 0 8px 30px rgba(0,0,0,0.5);" />
</p>

<h1 align="center">LINEA</h1>

<p align="center">
  <strong>Next-Generation Sovereign Cellular Dialer for Android 17 (API 37)</strong><br />
  <em>Industrial Glassmorphic Aesthetics • Native Telecom Framework • 100% On-Device • Zero Cloud Dependency</em>
</p>

<p align="center">
  <a href="https://developer.android.com/about/versions/17"><img src="https://img.shields.io/badge/Target%20Android-17%20(API%2037)-5C7C99?style=for-the-badge&logo=android&logoColor=white" alt="Target Android 17" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Min%20SDK-30%20(Android%2011)-2B313A?style=for-the-badge" alt="Min SDK 30" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.0" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-2024.11.00-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-5A8F6B?style=for-the-badge" alt="Apache 2.0 License" /></a>
</p>

<p align="center">
  <a href="#key-pillars--capabilities">Features</a> •
  <a href="#interface-showcase">Showcase</a> •
  <a href="#design-system">Design System</a> •
  <a href="#technical-architecture">Architecture</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="BUILD.md">Build Guide</a> •
  <a href="CONTRIBUTING.md">Contributing</a>
</p>

---

## Overview

**LINEA** (`com.ryanshelby.linea`) is a high-performance, fully sovereign, animated default cellular dialer replacement specifically targeted and optimized for **Android 17 (API 37)**. Engineered directly atop Android’s native Telecom framework (`InCallService`, `ConnectionService`, `PhoneAccount`, and `CallScreeningService`), LINEA manages genuine cellular calls, DTMF audio generation, multi-call conferencing, and carrier MMI commands with absolute mathematical privacy and **zero cloud dependency**.

Every contact index, availability metric, call screening filter, and caller ID lookup runs deterministically in local memory or encrypted SQLite databases. LINEA collects **no telemetry**, uses **no third-party tracking SDKs**, and connects to **no external servers**.

---

## Interface Showcase

<p align="center">
  <em>Experience an industrial glassmorphic interface designed with frosted titanium surfaces, tabular counters, and fluid transitions.</em>
</p>

| T9 Smart Dialpad | In-Call Active Calling | Contacts Directory | Contact Profile & Controls |
|:---:|:---:|:---:|:---:|
| <img src="docs/assets/screenshots/dialpad_t9.png" width="220" /> | <img src="docs/assets/screenshots/incall_dialing.png" width="220" /> | <img src="docs/assets/screenshots/contacts_directory.png" width="220" /> | <img src="docs/assets/screenshots/contact_dashboard.png" width="220" /> |
| *Zero-latency T9 matching with inline contact resolution* | *Hardware audio routes, DTMF pad, and call recording* | *A-Z haptic scrubber rail with dual-sync persistence* | *SIM affinity, per-contact rules, and complete deletion* |

| Dual SIM Manager | Call Screening Engine | Settings & Preferences Hub | About & System Diagnostics |
|:---:|:---:|:---:|:---:|
| <img src="docs/assets/screenshots/dual_sim.png" width="220" /> | <img src="docs/assets/screenshots/call_screening.png" width="220" /> | <img src="docs/assets/screenshots/settings_hub.png" width="220" /> | <img src="docs/assets/screenshots/about_foss.png" width="220" /> |
| *Per-slot carrier display, dBm telemetry & default routing* | *Deterministic regex, prefix rules & emergency bypass* | *Full control over proximity, audio, and call retention* | *Auditable FOSS specs, developer info & OS environment* |

---

## Key Pillars & Capabilities

### 1. Native Telecom Calling Engine
- **Full InCallService Integration**: Seamlessly binds to Android telephony hardware when configured as the system default dialer.
- **Carrier Audio Routing**: Dynamic routing across Earpiece, Speakerphone, Bluetooth SCO headsets, and Wired Headsets with proximity sensor ear-detection.
- **DTMF Audio Tone Synthesizer**: Low-latency dual-tone multi-frequency signaling with touch feedback for navigating automated phone menus.
- **Multi-Call & Conferencing**: Supports concurrent active calls, call swapping, call waiting, and 3-way conference call merging.

### 2. Instantaneous T9 Dialpad & Search
- Precomputed reverse prefix index for numbers `2`–`9` matching names, surnames, and company names.
- Instant search response matching both raw phone digits and alphanumeric names without UI lag.
- Clean clipboard auto-paste detection and one-tap dialing.

### 3. Contact Management & Deep Deletion
- **Dual-Write Architecture**: Bidirectionally synchronizes contacts with Android's system `ContactsContract` and the local Room database.
- **Complete Contact Purge**: Comprehensive deletion cleans local Room tables (`contacts`, `contact_numbers`, `contact_emails`, `contact_group_members`) and purges raw and aggregate contacts from Android's system content provider.
- **SIM Affinity**: Bind specific contacts to dial automatically through SIM 1 or SIM 2.

### 4. Smart Call Screening & Quiet Hours
- **On-Device Screening**: Intercepts spam, hidden/private numbers, and international calls before the phone rings.
- **Emergency Repeated-Call Override**: Automatically lets urgent callers through if the same number calls 3 times within 5 minutes, even when Quiet Hours are active.
- **Custom Rule Builder**: Wildcard patterns (e.g. `+1800*`), regex matching, and contact allow-lists.

### 5. Dual SIM Hardware Intelligence
- Detects installed physical SIMs and eSIM profiles with live carrier branding and signal telemetry.
- Optional 3s / 5s / 10s auto-call countdown dialog with cancelable SIM selector prompt.

### 6. Privacy Vault & Security
- **AES-256-GCM Vault**: 4-digit PIN-protected safe for sensitive contacts and private notes.
- **Master Key Security**: Cryptographic keys are anchored directly in Android's hardware-backed `AndroidKeyStore`.
- **Zero Cloud AI / Zero Telemetry**: Absolutely no network telemetry, Firebase, Crashlytics, or analytics SDKs bundled.

### 7. In-Call Recording & Voicemail
- Built-in call recorder capturing audio to protected local storage with real-time waveform visualization.
- Visual Voicemail interface with playback scrubbing, callback triggers, and read/unread status.

---

## Design System

LINEA rejects generic bright palettes in favor of a curated **Industrial Glassmorphic** theme:

```
Graphite Base (#0D0F12) ──> Frosted Panels (8% Alpha) ──> Titanium Hairlines (12% Alpha)
```

| Token | Specification | Hex / Value | Visual Application |
|---|---|---|---|
| **Background** | Deep Graphite | `#0D0F12` &rarr; `#1A1D21` | Edge-to-edge dark workspace |
| **Glass Surface** | Semi-transparent fill | `Color.White.copy(alpha = 0.08f)` | Cards, sheets, nav bars |
| **Glass Border** | Hairline stroke | `1.dp`, `Color.White.copy(alpha = 0.12f)` | Precision bounding box |
| **Titanium Accent** | Titanium Blue | `#5C7C99` | Primary actions, toggles, badges |
| **Success / Answer** | Sage Green | `#5A8F6B` | Call connect, incoming status |
| **Danger / Block** | Rust / Brick Red | `#B5473F` / `#8C3B35` | End call, delete, block rules |
| **Warning / Pin** | Amber Gold | `#D4A359` | Pinned items, emergency bypass |
| **Numerics** | Tabular Figures | `fontFeatureSettings = "tnum"` | Timers, dialpad digits, telemetry |

---

## Technical Architecture

```
                                  ┌──────────────────────────────┐
                                  │      Android Telecom API     │
                                  │  (InCallService / Telecom)   │
                                  └──────────────┬───────────────┘
                                                 │
                                                 ▼
┌─────────────────────────┐           ┌──────────────────────────┐           ┌─────────────────────────┐
│   Jetpack Compose UI    │ <───────> │       CallManager        │ <───────> │   CallScreeningEngine   │
│ (Screens, Sheets, Nav)  │ StateFlow │ (Audio, Calls, Telecom)  │  Events   │ (Rules, Quiet Hours)    │
└─────────────────────────┘           └──────────┬───────────────┘           └─────────────────────────┘
                                                 │
                                                 ▼
                                      ┌──────────────────────────┐
                                      │   ContactSyncRepository  │
                                      └──────────┬───────────────┘
                                                 │
                        ┌────────────────────────┴────────────────────────┐
                        ▼                                                 ▼
         ┌──────────────────────────────┐                  ┌──────────────────────────────┐
         │        Room Database         │                  │   System ContactsContract    │
         │ (11 Relational Local Tables) │                  │  (Android ContentResolver)   │
         └──────────────────────────────┘                  └──────────────────────────────┘
```

For in-depth architectural patterns, see [ARCHITECTURE.md](ARCHITECTURE.md).

---

## Getting Started

### Installation
1. Download the latest `app-debug.apk` from [GitHub Releases](https://github.com/MdSagorMunshi/Linea/releases).
2. Install the APK via ADB or your device file manager:
   ```bash
   adb install -r app-debug.apk
   ```

### Set LINEA as Default Dialer
To enable native call answering, incoming HUD notifications, and call management:
1. Open LINEA and navigate to **Settings** &rarr; **Permissions Architecture**.
2. Tap **Set as Default Dialer** and confirm the system dialog.
3. *Alternatively, set via ADB:*
   ```bash
   adb shell telecom set-default-dialer com.ryanshelby.linea
   ```

---

## Building from Source

### Prerequisites
- **JDK**: OpenJDK 17
- **Android SDK**: API 37 (Android 17) platform & Build-Tools 35.0.0
- **Build System**: Gradle 8.11+ (via `./gradlew`)

```bash
# Clone the repository
git clone https://github.com/MdSagorMunshi/Linea.git
cd Linea

# Compile Kotlin & Room KSP
./gradlew compileDebugKotlin

# Run unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
# Binary created at: app/build/outputs/apk/debug/app-debug.apk
```

For complete compilation flags, release signing, and telephony debugging commands, read [BUILD.md](BUILD.md).

---

## Documentation Sitemap

| Document | Description |
|---|---|
| [BUILD.md](BUILD.md) | Complete environment setup, compilation commands, and ADB debugging cheat sheet |
| [ARCHITECTURE.md](ARCHITECTURE.md) | System architectural layers, Room schema, Telecom state machine, and T9 algorithms |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Contribution guidelines, Git branching strategy, and pull request standards |
| [SECURITY.md](SECURITY.md) | Security policy, cryptographic architecture, and vulnerability disclosure |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | Contributor Covenant v2.1 community pledge and standards |
| [LICENSE](LICENSE) | Apache License, Version 2.0 legal terms |

---

## Developer & Official Support

- **Lead Developer**: Ryan Shelby
- **Official Support Email**: [ryn@disr.it](mailto:ryn@disr.it)
- **FOSS Git Repository**: [https://github.com/MdSagorMunshi/Linea.git](https://github.com/MdSagorMunshi/Linea.git)
- **Web Project Page**: [https://github.com/MdSagorMunshi/Linea](https://github.com/MdSagorMunshi/Linea)

---

## License

LINEA is released as Free and Open Source Software under the **[Apache License 2.0](LICENSE)**.

```
Copyright 2026 Ryan Shelby

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
