# Changelog

All notable changes to the **LINEA** cellular dialer project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2026-09-09

### Changed
- Bumped `versionCode` to `2` and `versionName` to `2.0.0`.
- Updated version display across **Settings** footer and **About** screen to reflect v2.0.0.
- Consolidated all v1.1.0 feature additions (waveform, contact posters, haptic profiles, gestures,
  quick-decline sheet, international time-zone preview, and next-gen glassmorphism call UI) into the
  official **v2.0.0 major release** on GitHub.

---

## [1.1.0] - 2026-09-09

### Added
- **Live Audio Waveform Visualizer**:
  - Implemented 32-bar multi-harmonic fluid liquid glass audio visualizer (`LiveAudioWaveform`) rendered on the active call screen.
  - Dynamically changes phase, amplitude, and color palette based on real-time call states:
    - *Active Call*: Radiant cyan & titanium blue harmonic speaking cadence.
    - *Muted*: Soft flatline amber glow with minimal amplitude.
    - *On Hold*: Rhythmic pulsing warning amber.
    - *Recording*: Vibrant crimson red glowing glass bars.
- **Full-Screen Frosted Contact Posters**:
  - Created `ContactPosterBackground` featuring photo blur (`20.dp`), subtle breathing scale animations, and layered vignette gradient scrims.
  - Integrated native `ContactPhotoHelper` 16MB LRU memory cache for 0-latency instant poster rendering with zero third-party dependencies.
  - Generative architectural monogram watermark fallback for contacts without stored photos.
- **Haptic Audio Dialpad Profiles**:
  - Added 4 selectable dialpad sound & haptic profiles in Settings:
    1. *Titanium Glass* (Default): Crisp dual-micro clicks (`PRIMITIVE_CLICK`) with high-fidelity DTMF tones.
    2. *Mechanical Relay*: Heavy tactile relay thump (`EFFECT_HEAVY_CLICK`) with prolonged acoustics.
    3. *Stealth*: Subtle near-silent micro-vibrations (`PRIMITIVE_TICK`) with muted tones.
    4. *Classic*: Standard legacy Android dialpad vibration.
- **"Flip to Silence" & Proximity Wave Gestures**:
  - Engineered hardware sensor manager (`CallGestureManager`) leveraging device accelerometer/gravity and proximity sensors.
  - *Flip to Silence*: Instantly silences incoming call ringer when the device is flipped face-down on a flat surface.
  - *Proximity Wave*: Wave hand over the top proximity sensor to mute the ringer touch-free.
  - *Battery Safe Lifecycle*: Sensors strictly register on incoming ringing and instantly unregister upon answer, reject, or disconnect for 0% background battery drain.
  - Both gesture toggles are available under Settings &rarr; **Motion & Call Gestures** and default to **OFF**.
- **Quick Decline Glass Action Sheet**:
  - Implemented swipe-up next-gen frosted glass bottom sheet (`QuickDeclineSheet`) with 1-tap quick SMS preset chips and custom reply text input.
  - Reject-with-message wired directly to Android Telecom `Call.reject(true, textMessage)`.
- **International Time Zone Preview & Country Detection**:
  - Created `InternationalCountryHelper` with prefix mapping across 40+ countries.
  - Live frosted pill on the dialpad displays country flag, destination name, live local time, and late-night warning badge (`🌙 Night in Country`).
  - Country and cellular badge displayed directly in incoming caller identity header.
- **Next-Gen Call Screen Glassmorphism**:
  - Overhauled both `IncomingCallScreen` and `InCallScreen` with layered frosted glass containers, glowing radial ring aura pulses, tactile spring physics, and high-contrast specular borders.

### Fixed
- **Signature Ringtone Audio Stream Routing**:
  - Corrected audio attribute usage in `MediaPlayer` and `RingtoneManager` to route strictly to `AudioAttributes.USAGE_NOTIFICATION_RINGTONE` with stream `AudioManager.STREAM_RING`.
  - Resolved issue where ringtone was improperly routed to `STREAM_MUSIC`, ensuring system ringer volume sliders directly control ring volume.

---

## [1.0.0] - 2026-09-08

### Added
- **Sovereign Android 17 Dialer Architecture**:
  - Built natively on Android Telecom framework (`InCallService`, `ConnectionService`, `CallScreeningService`).
  - Edge-to-edge support targeting Android 17 (API 37) with min SDK 30 (Android 11).
- **Industrial Glassmorphic Design System**:
  - Custom dark graphite gradients (`#0D0F12` &rarr; `#1A1D21`), frosted glass panels, hairline titanium borders, and tabular figures.
- **Dual SIM Hardware Intelligence**:
  - Live cellular telemetry (carrier name, network technology `5G`/`LTE`, dBm signal strength).
  - Configurable SIM affinity per contact and auto-dial countdown timer.
- **On-Device Spam Screening & Quiet Hours**:
  - Deterministic regex, prefix matching (`+1800*`), and emergency 3-call repeat override.
- **Private Encrypted Vault**:
  - AES-256-GCM encrypted contact notes and private contact isolation anchored in Android Keystore.
- **In-Call Audio Recording**:
  - On-device local call recording with playback scrubber and waveform visualizer.
- **T9 Smart Directory Indexing**:
  - Sub-millisecond prefix search across contact names and telephone numbers.
- **Export & Maintenance**:
  - RFC 4180 CSV call history export and configurable retention auto-cleanup.
