# Changelog

All notable changes to the **LINEA** cellular dialer project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.1.0] - 2026-09-11

### Added
- **Tactile Neumorphism (Soft UI) Overhaul**:
  - Replaced legacy UI styling with an architectural Neumorphic design system.
  - Engineered dual-shadow lighting engine (`Modifier.neumorphic()`, `NeumorphicCard`, `NeumorphicWell`, `NeumorphicButton`, `NeumorphicIconButton`, `NeumorphicTheme`) utilizing opposing top-left specular highlight and bottom-right ambient drop shadows via Android native canvas `Paint.setShadowLayer`.
  - Dialpad digit keys depress physically into sunken wells on tap with tactile spring dynamics (`ScaleCompressSpring`) and haptic confirmation.
  - Active call controls depress into illuminated debossed wells with active status rings.
  - Controls balanced into an ergonomic 3x2 grid situated in the natural bottom thumb zone.
  - Transparent in-call keypad overlay with dismiss-on-outside-tap gesture.
  - Replaced floating navigation bar with extruded pill container and sunken debossed active tab well.
  - Restyled all search bars, filter chips, call history tiles, contact cards, and bottom sheets to extruded and debossed physical surfaces.
- **Hardware Button Call Control & Silencing**:
  - *Volume Up / Down Silencing*: Pressing Volume Up or Volume Down once during incoming ringing immediately silences the ringtone and vibration without rejecting or ending the call, allowing the call to continue ringing normally.
  - *Double-Press Power to End Calls*: Rapidly pressing the hardware Power button twice terminates the current call across all call states: incoming ringing (rejects/ends), outgoing dialing/connecting (disconnects), and active/received calls (disconnects), with tactile haptic confirmation.
  - Protected with hardware debounce filtering (120ms threshold) and active call lifecycle binding.
- **Call History Quick-Action Swipe Drawer**:
  - Swipe left on any call history item to reveal a stationary quick-action drawer featuring Call, SMS, Add/View Contact, and Block action buttons.
  - Enhanced gesture physics with friction damping and auto-close on outside tap.
- **Native Contact Sheet & Reactive Add-Contact Drawer**:
  - Integrated Android native `ContactsContract` view and insert intents.
  - Reactive bottom drawer allowing 1-tap contact creation for unknown numbers or launching system contact cards directly.
- **Missed Call Visual Differentiation**:
  - Applied distinct high-visibility red badge and red text styling for the most recent unreturned missed call entries in call history.
- **Reactive Number Blocking & Contract Sync**:
  - Added immediate UI feedback (toast confirmation and badge state update) on block/unblock actions with bi-directional synchronization with Android's system `BlockedNumberContract`.

### Changed
- Renamed theme pill from "Light Glass" to "Light" to reflect pure Tactile Neumorphic design.
- Modernized About screen hero card with balanced FOSS architecture specifications and resolved vertical layout constraints.

### Removed
- **Call Recording & Accessibility Service**:
  - Completely removed call recording functionality, auto-record triggers, accessibility service declarations, rationale dialogs, and microphone permission dependencies due to Android AOSP audio isolation policies.
- **In-Call Audio Waving / ECG Waveform**:
  - Removed real-time audio waving/waveform ECG visualizer, background level monitors, and in-call waveform toggle preferences to eliminate mic contention and optimize active call UI performance.

### Fixed
- **Call Log Deduplication**:
  - Eliminated duplicate entries in Call History by grouping and deduplicating records by normalized phone number, call type, and timestamp clustering, with instant state synchronization on database changes.
- **About Screen Layout**:
  - Resolved Compose nested column measurement expansion bug that caused the hero card to balloon vertically.
- **Telecom Number Normalization**:
  - Sanitized telephone dial strings and normalized E.164 URIs across contact lookups, dialer actions, and system intent launches.

---

## [2.0.0] - 2026-09-09

### Changed
- Bumped `versionCode` to `2` and `versionName` to `2.0.0`.
- Updated version display across **Settings** footer and **About** screen to reflect v2.0.0.
- Consolidated all v1.1.0 feature additions (waveform, contact posters, haptic profiles, gestures,
  quick-decline sheet, international time-zone preview, and next-gen call UI) into the
  official **v2.0.0 major release** on GitHub.

---

## [1.1.0] - 2026-09-09

### Added
- **Medical ECG Heartbeat Voice Visualizer**:
  - Implemented medical ECG / heartbeat style glowing voice visualizer line (`LiveAudioWaveform`) directly on the active call screen without any background boxes, borders, or text.
  - Dynamically modulates continuous P-Q-R-S-T medical heartbeat waves based on real-time audio volume from caller and receiver.
  - Features an absolute straight horizontal line when silent (no audio detected) or muted/held.
  - Added user toggle in **Settings &rarr; Appearance & Motion** ("In-Call Heartbeat Waveform") defaulting to **ON**, allowing users on low-end devices to turn it off.
- **Full-Screen Cinematic Contact Posters**:
  - Created `ContactPosterBackground` featuring photo blur (`20.dp`), subtle breathing scale animations, and layered vignette gradient scrims.
  - Integrated native `ContactPhotoHelper` 16MB LRU memory cache for 0-latency instant poster rendering with zero third-party dependencies.
  - Generative architectural monogram watermark fallback for contacts without stored photos.
- **Haptic Audio Dialpad Profiles**:
  - Added 4 selectable dialpad sound & haptic profiles in Settings:
    1. *Tactile Neumorphic* (Default): Crisp dual-micro clicks (`PRIMITIVE_CLICK`) with high-fidelity DTMF tones.
    2. *Mechanical Relay*: Heavy tactile relay thump (`EFFECT_HEAVY_CLICK`) with prolonged acoustics.
    3. *Stealth*: Subtle near-silent micro-vibrations (`PRIMITIVE_TICK`) with muted tones.
    4. *Classic*: Standard legacy Android dialpad vibration.
- **"Flip to Silence" & Proximity Wave Gestures**:
  - Engineered hardware sensor manager (`CallGestureManager`) leveraging device accelerometer/gravity and proximity sensors.
  - *Flip to Silence*: Instantly silences incoming call ringer when the device is flipped face-down on a flat surface.
  - *Proximity Wave*: Wave hand over the top proximity sensor to mute the ringer touch-free.
  - *Battery Safe Lifecycle*: Sensors strictly register on incoming ringing and instantly unregister upon answer, reject, or disconnect for 0% background battery drain.
  - Both gesture toggles are available under Settings &rarr; **Motion & Call Gestures** and default to **OFF**.
- **Quick Decline Neumorphic Action Sheet**:
  - Implemented swipe-up extruded bottom sheet (`QuickDeclineSheet`) with 1-tap quick SMS preset chips and custom reply text input.
  - Reject-with-message wired directly to Android Telecom `Call.reject(true, textMessage)`.
- **International Time Zone Preview & Country Detection**:
  - Created `InternationalCountryHelper` with prefix mapping across 40+ countries.
  - Live tactile pill on the dialpad displays country flag, destination name, live local time, and late-night warning badge (`🌙 Night in Country`).
  - Country and cellular badge displayed directly in incoming caller identity header.
- **Next-Gen Call Screen Architecture**:
  - Overhauled both `IncomingCallScreen` and `InCallScreen` with tactile containers, glowing radial ring aura pulses, tactile spring physics, and high-contrast specular borders.

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
- **Tactile Physical Design System**:
  - Custom dark graphite workspace (`#181B20` &rarr; `#1E2228`), extruded panels, debossed wells, and tabular figures.
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
