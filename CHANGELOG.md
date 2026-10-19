# Changelog

All notable changes to the **LINEA** cellular dialer project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.6.1] - 2026-10-19

### Fixed & Optimized
- **Dual-SIM Identification & Telephony Resolution Chain**:
  - **Deterministic SIM Slot Resolution**: Implemented a robust 5-tier resolution chain in `SimAccountResolver` mapping `Call` / `CallLog` &rarr; `PhoneAccountHandle` &rarr; `SubscriptionInfo` &rarr; physical/logical 0-based SIM slot index (`0` for SIM 1, `1` for SIM 2) &rarr; display label. Replaced fragile default-SIM assumptions, arbitrary ID sorting, and last-dialed guesswork with verified subscription mappings.
  - **Elimination of Speculative Fallbacks & Phantom SIM 2**: Completely eradicated speculative `callCapablePhoneAccounts` fallback loops that erroneously synthesized phantom "SIM 2" indicators on single-SIM devices and emulators. Active multi-SIM detection is now strictly bound to hardware-verified `SubscriptionManager.activeSubscriptionInfoList`.
  - **Clean Single-SIM Interface Degradation**: Enforced a zero-clutter rule for single-SIM devices across the entire UI. SIM selector pills, badges, and labels are fully hidden across the dialpad, recent calls history, call detail sheets, incoming call overlay, active call screen, and missed call notifications when only one SIM is present.
  - **Persistent In-Call SIM Indicators**: Added spring-animated glassmorphic SIM indicator pills ("SIM 1" / "SIM 2") under caller identity during incoming calls, outgoing dialing, and connected calls. The indicator cleanly persists across in-call state changes including Hold, Mute, Speakerphone, and Bluetooth routing without shifting action controls.
  - **System Call Log & Historical Record Attribution**: Enhanced `CallLogRepository` to query system `PHONE_ACCOUNT_ID` and `PHONE_ACCOUNT_COMPONENT_NAME`. Integrated multi-tier fallback parsing to resolve historical calls and OEM-specific call log account handles (matching ICCID substrings and numeric subscription IDs).
  - **Dynamic Subscription & Hot-Swap Monitoring**: Integrated `SubscriptionManager.OnSubscriptionsChangedListener` and `ACTION_SIM_STATE_CHANGED` broadcast receivers to instantly update multi-SIM state, active slots, and UI visibility when physical SIMs or eSIM profiles are added, removed, or toggled at runtime.
  - **Missed Call Notification Attribution**: Formatted missed call notifications with `" • SIM X"` attribution badges exclusively on dual-SIM devices, preventing notification clutter on single-SIM setups.
  - **Unit Test Coverage**: Added comprehensive test suite in `SimAccountResolverTest` verifying slot index resolution, single-SIM hiding, dual-SIM attribution, eSIM + physical SIM setups, call log ICCID/subId parsing, emergency calls, and hot swaps.

---

## [2.6.0] - 2026-10-19

### Added
- **Navbar Customization System**:
  - **Navbar Style Selector**: Choose between the compact **Floating Circle** (default) with tap-expansion, 5-second auto-collapse, and 4-way gesture flick; or the **Expanded Pill Bar** with permanent full-width frosted glass and instant one-tap tab switches.
  - **Dynamic Circle Sizing**: Customize the physical diameter and thumb touch footprint of the collapsed floating button across 4 distinct sizes: Compact (48dp), Standard (56dp - Default), Comfortable (64dp), and Large (72dp).
  - **Animation & Motion Physics**: Tailor morphing and stretch spring dynamics with selectable physics models: Bouncy Spring (Default), Smooth Fluid, Snappy Quick, and Minimal Fade.
  - **Interactive Live Sandbox**: In-settings real-time sandbox preview allowing users to immediately test navbar morphing, selection physics, and gesture mechanics before navigating out.
  - **Gesture Quick-Reference Guide**: Visual instructional cards explaining Tap expansion, 4-Way Gesture Flick (direct tab jumps without expanding), and Triple-Tap persistence toggle.
  - **Settings Integration & Search Engine**: Added a dedicated Navigation Bar entry under Appearance & Motion with live subtitle indicators, mode badges, and full indexing in the Settings search engine.

### Fixed & Optimized
- **Incoming Call Ringtone Resilience & Asset Transcoding**:
  - **Standard AAC-LC Audio Transcoding**: Re-encoded embedded ringtone assets (`ring_glass.m4a`, `ring_minimal.m4a`) to standard AAC-LC / 44.1kHz stereo audio format, resolving intermittent playback failures and decoder stalls on strict hardware media decoders.
  - **3-Tier Ringer Fallback Chain**: Built resilient audio fallback in `IncomingCallActivity` attempting raw resource player &rarr; system ringtone URI stream &rarr; default notification alert sound, ensuring an incoming call is never silent even if a custom sound resource fails to load.
  - **Telecom Silence Grace Period Extension**: Extended startup silence grace period from 600ms to 2000ms upon initial Telecom connection to eliminate ringer startup race conditions and prevent spurious volume key events from silencing the incoming call ringtone.

---

## [2.5.0] - 2026-10-16

### Added
- **Country-Based Call Blocking System**:
  - **Country Blacklist / Whitelist**: Added a comprehensive country-based call filter with strict "Whitelist Only" and "Blacklist Only" modes.
  - **High-Precision Country Detection**: Integrated Google's `libphonenumber` to accurately identify caller countries from incoming E.164 and localized numbers. Disambiguates shared calling codes seamlessly (e.g., distinguishing between USA and Canada under `+1`, or Russia and Kazakhstan under `+7`).
  - **Granular Controls**: Specific countries under a shared dialing code can be whitelisted even if the entire code is blacklisted.
  - **Contact & Domestic Exemptions**: Saved contacts and calls originating from the user's domestic SIM country bypass blocking automatically to ensure critical calls are never missed.
  - **Management UI**: New dedicated "Country Call Blocking" settings interface with interactive flag-based selectors, search integration, and a status dashboard on the main Blocking Screen.
  - **Safe Edge Case Handling**: Private, unknown, and withheld caller IDs bypass country checks to avoid false classifications, delegating to the existing unknown caller filter rules.

### Fixed & Optimized
- **Incoming Call Audio Pipeline & Ringer**:
  - **Audio Attributes & Stream**: Configured ringtone playback strictly with `USAGE_NOTIFICATION_RINGTONE` and `STREAM_RING`, properly requesting transient audio focus with ducking.
  - **Auto-Silencing Elimination**: Resolved internal volume key silencing race conditions and state desynchronization that previously caused incoming calls to ring silently.
  - **Vibration Synchronization**: Ensured haptic vibration patterns synchronize cleanly with ringtone playback and immediately halt upon call answer, reject, or disconnect.
- **Telecom Logging & Call History**:
  - **Accurate Duration Tracking**: Computed call duration using elapsed monotonic connect time rather than wall-clock differences, preventing duration errors during screen-off sleep.
  - **Direction Classification**: Fixed direction assignment on call removal to ensure missed, incoming, and outgoing calls are categorized and persisted to Room without dropped records.
- **Default Dialer Role Power Exemption**:
  - **Native Role Exemption Integration**: Recognized Android's built-in `ROLE_DIALER` unrestricted battery usage exemption (`PowerExemptionManager.REASON_ROLE_DIALER`). Removed the redundant "Background Execution ATTENTION" warning when LINEA is the active default dialer.
  - **Settings Health Card**: Status banner now reflects full readiness with a green `ACTIVE` state ("Unrestricted: granted automatically by Default Dialer role") and cleanly collapses into the optimal checkmark header.
- **System Battery Drain Optimizations**:
  - **NFC Radio Transmitter Scoping**: Removed continuous background reader mode polling from `MainActivity.onResume()`. Scoped NFC hardware polling strictly to the `DisposableEffect` of `LineaBeamSheet`, preventing constant 2–3Hz radio transmitter pulsing.
  - **Sensor Hardware Leak Prevention**: Fixed a race condition in `CallGestureManager` by cancelling asynchronous startup jobs upon `stopListening()`, ensuring accelerometer and proximity sensors never remain active after quick call termination.
  - **Proximity Sensor Power Rail Release**: Changed proximity wake lock release to drop immediately upon call disconnect (`release(0)`), preventing the physical proximity sensor rail from holding power for up to 2 hours when placed face-down or pocketed.
  - **Cleanup Worker Constraints**: Added `setRequiresDeviceIdle(true)` and `setRequiresBatteryNotLow(true)` to periodic call history maintenance work.
- **Missed Call Notification Deduplication**:
  - **Phone Number Digit Normalization**: Normalized digits to ensure raw telecom handles (`15551234567`) and system-formatted numbers (`1 555-123-4567`) produce identical notification IDs.
  - **Suppression Window**: Added a 15-second deduplication suppression window in `CallNotificationManager` to prevent duplicate alerts.
  - **Broadcast Coordination**: Updated `MissedCallReceiver` to suppress redundant platform `ACTION_SHOW_MISSED_CALLS_NOTIFICATION` broadcasts when `InCallService` has already posted, while maintaining asynchronous contact lookup fallback.

---
## [2.4.1] - 2026-09-30

### Fixed
- **QR Code Scanner Physical Device Camera Crash & Blank Screen**:
  - **R8 / ProGuard Minification Protection for ML Kit**: Added `-keep` rules for Google ML Kit vision components, `ComponentRegistrar` discovery services, CameraX, and ZXing in `proguard-rules.pro`. This prevents R8 from stripping ML Kit reflection in release builds, resolving the physical device crash (`Attempt to read from field 'lt3 ko3.a' on a null object reference in method 'zq3 nx3.d(wj)'`).
  - **Safe Initialization & Real-Time Offline ZXing Fallback**: Safely encapsulated `BarcodeScanning.getClient(...)` inside try/catch so ML Kit initialization failure never prevents camera hardware binding. Implemented an offline fallback analyzer using CameraX 8-bit Y-plane luminance (`com.google.zxing.PlanarYUVLuminanceSource`), allowing immediate offline QR scanning without Google Play Services.
  - **LifecycleOwner Resolution**: Traversed context wrapper hierarchy (`Context.findActivity()`) to bind CameraX directly to the hosting `ComponentActivity` lifecycle (`effectiveLifecycleOwner`), eliminating blank preview frames caused by synthetic dialog window lifecycles on OEM skins (Samsung OneUI, Xiaomi HyperOS, ColorOS).
  - **Resilient 3-Tier Surface Binding Strategy**: Replaced rigid 4:3 `ResolutionSelector` with automatic multi-tier fallback (720p `ResolutionStrategy` &rarr; Android CDD guaranteed default stream combination &rarr; Preview-only fallback), preventing `IllegalArgumentException` surface combination crashes across diverse camera sensors and hardware levels (`LEGACY`, `LIMITED`, `FULL`).
  - **Exception & Error Handling**: Replaced silent error swallowing with structured logging and an interactive error state UI featuring **"Retry Camera"** and **"Scan Photo"** buttons.
  - **Camera Hardware Resource Cleanup**: Implemented `cameraProviderRef` with explicit `cameraProvider.unbindAll()` inside `DisposableEffect.onDispose` to prevent camera device locks when dismissing and reopening the sheet.
  - **Live Streaming Synchronization & Permissions**: Observed `PreviewView.previewStreamState` to render a smooth loading spinner until frames actively stream, and added an automatic camera permission prompt with lifecycle resume refresh.

- **Incoming Call Hardware Button Behavior**:
  - **Volume Up/Down Silences Ringtone**: Pressing either volume button during an incoming call immediately silences the ringtone. Reduced the internal grace period from 2000ms to 600ms so that volume key presses register almost instantly after ringing begins.
  - **Power Button Single Press Silences Ringtone**: A single power button press during an incoming call now reliably silences the ringtone. Removed the proximity sensor gate that was previously blocking power button silencing when the phone was near the user's face or in a pocket.
  - **Power Button Double Press Ends Call**: Two rapid power button presses (within 1 second) terminate the current call — incoming, outgoing, or active. Added `ACTION_SCREEN_ON` broadcast listener to detect the second press (screen off → screen on = double press), since Android does not deliver `KEYCODE_POWER` to applications.

- **Full Application Audit & Multi-Phase Bug Fixes (Phases 1–3)**:
  - **Telecom & In-Call Stability**:
    - Resolved `CallManager` double-dispose crash hazard on rejected or disconnected calls.
    - Added missing `FOREGROUND_SERVICE_PHONE_CALL` handling on Android 14+ (API 34+).
    - Fixed audio route desync when toggling between speakerphone and earpiece under Android Telecom framework.
    - Fixed SIM account selection race condition when placing calls during dual-SIM state transitions.
    - Corrected in-call duration timer drift when the device is locked or screen turns off.
    - Added fallback audio sources for call recording when `VOICE_CALL` is restricted by OEM hardware.
  - **Security & Vault Protection**:
    - Hardened `PrivateSafeScreen` with mandatory biometric re-authentication and strict lifecycle re-lock upon app backgrounding.
    - Secured `PrivateVaultSecurityManager` Master PIN verification against timing attacks using constant-time string comparison (`MessageDigest.isEqual`).
    - Fixed contact deletion cleanup to prevent orphaned entries in private call notes and speed dial mappings.
  - **UI/UX & Performance Optimization**:
    - Eliminated Recent call list scroll jank by memoizing date header calculations.
    - Fixed DTMF tone overlap when rapidly pressing dialpad keys.
    - Resolved keyboard dismissal and highlight desync in Settings search results.
    - Added number formatting normalization before saving to blocked numbers list.
    - Added robust error handling and validation for corrupt JSON files during backup and restore.
    - Fixed light theme text contrast on digital card profile inputs and replaced placeholders with local vector social icons.

### Added
- **NFC Beaming & Digital Card Enhancements**:
  - Added closed-app mutual beam processing, tag detection diagnostics, and retry workflows.
  - Added "Always Share" mode and persistent digital card profile fast-cache synchronization in the Settings header.
  - Dynamically displayed the installed application version in the Settings footer.

### Optimized
- **APK Size Reduced from 73.7 MB to ~30 MB**: Enabled R8 full-mode code shrinking and resource optimization, eliminating unused classes, dead code paths, and redundant bundled assets — cutting the release APK size by over 58%.

---

## [2.4.0] - 2026-09-23

### Added
- **LiNEA Beam: 1-Tap Mutual NFC Contact Sharing & HCE Protocol**:
  - **Bidirectional SWAP APDU (`INS = 0xA5`)**: Engineered peer-to-peer exchange over ISO-DEP; in a single physical phone bump, Phone A sends its contact card to Phone B and receives Phone B's contact card in the response, displaying the animated contact card popup simultaneously on both devices.
  - **Host Card Emulation (HCE) & NDEF Universal Compatibility**: Dual AID registration supporting LiNEA Custom AID (`F0010203040506`) for high-speed bumps and NFC Forum Type 4 Tag NDEF Application (`D2760000850101`) for compatibility with any NFC device or card.
  - **Physical NFC Tag Programming**: Capability to program blank or rewritable physical NFC stickers, cards, or key fobs with the user's contact card.
  - **Hardware Fallback & Non-NFC Device Support**: Explicit `NOT SUPPORTED` status badges and warning notifications for devices lacking NFC hardware, with seamless QR code fallback preserved.

- **Personal Profile Card Setup in Settings ("My Digital Card")**:
  - Dedicated profile editor in Settings supporting Full Name, Phone Number, Email Address, Compressed Profile Photo, and 2 simultaneous Social Usernames (Instagram, Telegram, GitHub, Reddit).
  - **Intelligent Photo Compression Pipeline**: Automatically downscales picked images to max $128 \times 128$ px and compresses to 70% quality JPEG Base64 (~2–4 KB) for sub-100ms NFC transmission.
  - **Live Holographic Digital Card Preview**: Real-time holographic glass card with reactive neon borders and dynamic avatar, phone, email, and social pills.
  - **Dual Simultaneous Social Handles**: Configurable platform pickers with signature brand palettes and auto-prefixing (`@` for Instagram, Telegram, GitHub; `u/` for Reddit).

- **Rich Animated Beamed Contact Sheet Popup**:
  - Automatically triggered upon physical bump, displaying decoded photo avatar with rotating multi-spectral glowing ring.
  - Quick action rows for phone numbers (copy / call) and email (copy / launch mail client).
  - Interactive clickable social badges (Instagram, Telegram, GitHub, Reddit) launching the respective profile app or URL.
  - One-tap "Save Contact" persisting photo bytes, numbers, email, and social notes directly into system contacts and local Room database.

- **Orbital Speed Dial ("Planetary Dial") & Gesture Separation**:
  - **Hold (~400ms) on Call Button**: Blossoms a futuristic radial speed dial with up to 7 favorite contacts in an upper arc within thumb reach, featuring laser tether guidance, magnetic node capture, haptic feedback, and instant drag-and-release dialing.
  - **Clean Gesture Separation**: Call Button hold is strictly dedicated to Orbital Speed Dial, while double-tapping (when dialpad is empty) or triple-tapping (anytime) triggers the Fiery Magma Easter Egg.



### Fixed
- **LiNEA Beam NFC Reliability**:
  - Implemented a chunked APDU protocol to safely transfer payloads exceeding the standard 261-byte `IsoDep` limit.
  - Engineered adaptive duty-cycling (320ms reader / 220ms listen) to prevent RF collisions when two devices scan simultaneously.
- **Dialer Short Code Crash**:
  - Fixed a `StringIndexOutOfBoundsException` triggered when dialing specific 3-digit numbers starting with "1" (e.g., "121") caused by an incorrect string length check during regional area code parsing.

---

## [2.3.0] - 2026-09-20

### Added
- **Bespoke Stylish QR Code Contact Sharing & Real-Time ML Kit Scanner**:
  - **Connected Liquid Capsule Matrix Renderer**: Custom-designed organic QR matrix where adjacent data modules seamlessly connect into continuous rounded capsules and fluid ribbons ($R = \text{moduleSize} \times 0.42$), eliminating standard harsh squares and generic disconnected dots while strictly complying with the ISO/IEC 18004 QR specification.
  - **Styled Squircle Finder Eyes & Gradient Pupil Cores**: Position detection patterns engineered with smooth squircle contours and vibrant gradient centers matching the selected design theme.
  - **Central Contact Medallion Badge**: Embedded initial monogram medallion framed with a glowing accent rim, protected by QR Level Q error correction (25% structural data recovery).
  - **Multi-Theme Color Palettes**: 5 tactile color schemes—Titanium Cyan, Emerald Mint, Cyber Amber, Neon Orchid, and Pure White.
  - **Sub-5ms Google ML Kit Vision Barcode Analysis**: Real-time CameraX frame analysis streaming `InputImage.fromMediaImage` directly into Google ML Kit Barcode Scanning with single-digit millisecond latency across difficult lighting conditions and viewing angles.
  - **Strict LiNEA QR Code Filtering & Random Code Rejection**:
    - Scanner strictly detects and decodes authentic LiNEA QR codes (`linea://contact` and `linea://` URIs), ignoring random QR codes (websites, Wi-Fi setups, generic barcodes, plain text).
    - Prevents false triggers and accidental scans from random QR codes in view.
  - **Intelligent Multi-QR Code Prioritization**:
    - When multiple QR codes are simultaneously visible in the camera viewfinder or in an imported photo, the scanner automatically filters and specifically targets the authentic LiNEA QR code, completely ignoring surrounding non-LiNEA codes.
  - **Instant One-Tap Number Copy & Per-Row Copying**:
    - Added a prominent, styled frosted-glass **Copy** button to the decoded contact card alongside "Scan Again" for instant clipboard copying with tactile haptic feedback and toast confirmation.
    - Added individual tap-to-copy capability on each phone number row inside the contact card.
  - **Tap-to-Focus, Flashlight Torch & Photo Gallery Scanner**: Full camera controls plus instant photo gallery decoding via `InputImage.fromFilePath` and dual-pass ZXing fallback (with informative feedback when an imported photo contains no LiNEA QR code).
  - **Dialpad & Contact List Fast Access**: Integrated QR Scanner buttons directly into the Cellular Dialpad header and Contacts directory header, plus contact sharing action in contact profiles.

- **Recent Call History Ultra-Compact 1-Second Hold-to-Copy Sheet**:
  - Long-pressing any contact or call log item for 1 second on the **Recent** history page (both Feed and Session views) summons an **ultra-compact**, tactile frosted glass sheet.
  - Minimalist layout featuring the caller avatar circle and name horizontally aligned, followed by a sleek card displaying `PHONE NUMBER`, the full number, and a `[ ⎘ Copy ]` pill badge.
  - Stripped away unnecessary headers, subtitles, and redundant action rows for an ultra-clean, minimal footprint.
  - Tapping anywhere on the card or badge copies the phone number to the clipboard with haptic feedback, shows a confirmation toast, and smoothly dismisses the sheet.
  - Touch-slop gesture safety canceling hold triggers during vertical list scrolling or horizontal swiping.

- **Post-Call Smart Action HUD (Transient Quick-Action Card)**:
  - Engineered an optional, tactile Neumorphic bottom sheet HUD card (`PostCallHudCard`) presented when any call concludes (incoming, outgoing, missed, or simulated). **Disabled by default (opt-in)** to preserve existing dialer flows unless explicitly activated by the user.
  - Provides four instant, high-utility post-call actions before the in-call window dismisses:
    - ⏰ **1-Tap Callback Reminders**: Instant scheduling chips (`15 min`, `1 hour`, `Tomorrow 9 AM`) backed by `CallbackReminderScheduler` and high-priority Android alarm notifications.
    - 📝 **Quick Scratchpad Note**: Instant inline note entry with one-tap contextual tag chips (`"Follow-up needed"`, `"Sent details"`, `"Call again"`, `"Important"`), saved directly into Room database via `CallNoteDao`.
    - 💬 **Quick Follow-Up SMS**: Quick-launch message chips (`"Can't talk right now, I'll call you shortly."`, `"Could you send me the details via SMS?"`, `"Thanks for the call!"`, `"Let's follow up on this tomorrow."`) launching the default SMS composer via `Intent.ACTION_SENDTO`.
    - 🛡️ **Instant Block / Mark Spam**: One-tap defensive action writing directly to Room `BlockedNumberDao` and Android's `BlockedNumberContract`.
  - **Dynamic Visual Summary Header**: Displays caller avatar, contact name or phone number, call status badge (incoming/outgoing with exact duration or missed/unanswered status), and offline geographic location badge powered by `OfflineCallerIdEngine`.
  - **Countdown Bar with Intelligent Interaction Freeze & Manual Mode**: Configurable auto-dismiss countdown timer with an animated linear progress indicator that automatically pauses indefinitely when user touches input fields, selects reminder chips, or composes notes.
  - **Granular Post-Call Time Customization**:
    - Precision Neumorphic Swipeable Slider: intuitive 1-second granularity between 3s and 30s with real-time feedback indicator (`"Auto-dismisses HUD after X seconds"`). Replaces cluttered duplicate preset buttons with a single unified, responsive slider control.
  - **Customizable Calling Preferences & DataStore Persistence**: Added `postCallHudEnabled` toggle (default `false`) and `postCallHudDurationSeconds` (persisting custom 3s..30s slider values) in Settings.
  - **Intelligent Search Indexing**: Fully indexed in `SettingsSearchEngine` under `SettingsCategory.CALLING` with both `post_call_hud` and dedicated `post_call_hud_time` entries featuring interactive one-tap duration cycling and search keywords ("post call time", "customize post call time", "hud time", "hud duration", "action card", "reminder", "scratchpad", "follow-up", "auto-dismiss").
  - Comprehensive unit test coverage (`PostCallHudTest`) validating duration formatting, caller ID location resolution, reminder timestamp calculation, note tag concatenation, search indexing, duration cycling, and verification that the feature defaults to disabled.

- **100% Offline Worldwide Regional Caller ID & Carrier Intelligence**:
  - Engineered a 100% offline, privacy-first caller ID and location intelligence engine (`OfflineCallerIdEngine`) requiring zero internet connection and zero location permissions.
  - Complete worldwide coverage encompassing **all 248 sovereign nations, UN observer states, island protectorates, and overseas territories** (100% of ISO 3166-1 catalog).
  - Smart international dialing detection without requiring the `+` sign (e.g. `880...`, `54...`, `351...`, `7701...`, `1416...`, `91...`, etc.).
  - Smart domestic SIM country integration (`SimCountryDetector`): detects user's active SIM country code via TelephonyManager without requesting GPS location or internet permissions to accurately disambiguate domestic local dialing from foreign international codes.
  - Granular North American Numbering Plan (NANP, `+1`) coverage: cleanly distinguishes Canadian numbers (`🇨🇦`) from United States numbers (`🇺🇸`), with sub-national city and metropolitan identification (Toronto, Montreal, Vancouver, New York, Chicago, San Francisco, Dallas, Houston, Miami, etc.).
  - Prefix disambiguation for shared country codes: `+7` distinguishes Kazakhstan (`🇰🇿`, mobile prefixes 76, 77 with operator detection for Kcell/Activ, Beeline KZ, Tele2 KZ, Altel, and city detection for Astana/Almaty) from Russia (`🇷🇺`).
  - Sub-national operator & mobile carrier recognition:
    - Bangladesh (`+880`): Grameenphone, Robi, Banglalink, Teletalk
    - Pakistan (`+92`): Jazz, Telenor, Zong, Ufone
    - United Arab Emirates (`+971`): e&, du
    - Saudi Arabia (`+966`): stc, Mobily, Zain
    - Nigeria (`+234`): MTN, Airtel, Glo, 9mobile
    - Kenya (`+254`): Safaricom, Airtel, Telkom
    - Philippines (`+63`): Globe, Smart, DITO
    - Regional metro and city prefixes for Germany (`+49`), France (`+33`), United Kingdom (`+44`), Australia (`+61`), India (`+91`), and Japan (`+81`).
  - Consistent international representation: Palestine (`🇵🇸`, `PS`) for `+970` and `+972` (with dynamic ISO mapping from `IL` to `PS`); China (`🇨🇳`, `CN`) for `+886` with regional location `"Taiwan, China"` and badge `"TAIWAN, CHINA"` (with dynamic ISO mapping from `TW` to `CN`).

- **Live International Timezone Preview with 6 Time-of-Day States & Custom Vector Icons**:
  - Integrated dynamic timezone preview pill (`InternationalPreviewPill`) on the Cellular Dial Pad, displaying the destination's live local time, GMT offset, and time-of-day contextual state in real time.
  - Verified canonical IANA timezone mappings across all 248 jurisdictions worldwide (`InternationalCountryHelper`).
  - Replaced generic night warnings with 6 rich, day/night contextual states featuring custom-designed vector SVG drawables:
    - 🌅 **Early Morning** (`05:00 - 07:59`): `ic_time_early_morning.xml` (dawn sunrise with rising rays & horizon)
    - ☀️ **Morning** (`08:00 - 11:59`): `ic_time_morning.xml` (full morning radial sun)
    - 🔆 **Midday** (`12:00 - 13:59`): `ic_time_midday.xml` (overhead solar peak with diamond flares)
    - 🌤️ **Afternoon** (`14:00 - 16:59`): `ic_time_afternoon.xml` (warm descending sun arc)
    - 🌆 **Evening** (`17:00 - 20:59`): `ic_time_evening.xml` (twilight dusk sun sinking into horizon)
    - 🌙 **Night** (`21:00 - 04:59`): `ic_time_night.xml` (crescent moon with twinkle stars)
  - Single-line fluid pill layout preventing text wrapping with smooth animated transitions.

- **Tactical "Escape Call" Simulator (Stealth Fake Incoming Call Generator)**:
  - Built a tactical incoming call simulator (`EscapeCallSheet`, `EscapeCallManager`, `EscapeCallReceiver`, `FakeCallActivity`) designed for discreet, realistic interruptions.
  - Simulates authentic full-screen incoming calls identical to native Linea calls, including contact avatar, caller name, phone number, and location labels.
  - Configurable deployment timers: Instant / Immediate, 5 seconds, 15 seconds, 30 seconds, 1 minute, 5 minutes, or custom countdown timer.
  - One-tap tactical scenario presets for rapid deployment: "Boss / Work Emergency", "Family Urgent", "Doctor / Clinic", "Delivery Courier".
  - Authentic audio and vibration playback with customizable ringtone selection and vibration patterns.
  - Interactive voice scenario simulation during active answered escape calls with realistic synthetic audio and speech pause cadence.
  - Dedicated Help Menu and configuration settings: custom prefix, dial code triggers, caller profiles, and delay preferences.
  - Fully indexed and discoverable in Settings Search via queries like "escape", "fake call", "rescue", "emergency", "simulator", "stealth call", etc., with direct in-place sheet navigation.

- **Powerful, Smart Search in Settings**:
  - Implemented a dedicated, intelligent search bar exclusively for the **Settings** screen conforming to Linea's tactile Neumorphic design system (`NeumorphicWell`, `LineaColors`, `LineaTypography`).
  - Indexed all 33+ configurations, toggles, preferences, and sub-screens across 10 specialized categories (General, Calling, Audio & Sound, Motion & Gestures, Screening & Security, Appearance, History & Storage, Telecom, Permissions, About).
  - Built an intelligent scoring and ranking engine with extensive synonym resolution (e.g., "mute" -> Flip to Silence, "dark" -> Color Theme, "vvm" -> Visual Voicemail, "blacklist" / "spam" -> Call Screening & Blocking, "signal" / "5g" -> Cellular Diagnostics, "pocket" -> Gestures).
  - Designed interactive search result cards with category badges, icons, subtitles, and direct in-place controls (live `Switch` toggles, sub-screen navigation arrows, selection pills, and action triggers).
  - Added a tactile empty state with "No Settings Found" messaging and interactive suggested query pills ("Ringtone", "Dual SIM", "Theme", "Gestures", "Flip", "Haptics", "Blocking", "Voicemail", etc.) that auto-fill the search bar on tap.
  - Added unit test suite `SettingsSearchEngineTest` verifying exact matches, prefix matching, synonym keyword resolution, multi-token queries, case insensitivity, empty query handling, and score ranking.

### Fixed
- **In-Call Keypad Sheet Call Termination**:
  - Embedded a dedicated red `EndCallButton` directly below the 4x3 digit grid in `InCallKeypadSheet`, resolving an ergonomic safety issue where opening the keypad during an active call (e.g. for IVR/automated menus) faded out the lower controls and required users to dismiss the keypad before being able to disconnect.
- **Call Screening & Blocking Screen FAB Occlusion**:
  - Added `navigationBarsPadding()` and adjusted margins on `FloatingActionButton`, along with expanding bottom list content padding (32.dp) and bottom spacer (96.dp) in `BlockingScreen`, ensuring rule cards and trailing unblock/delete buttons are never occluded by the FAB.
- **Settings Search Suggestion Chip Hitboxes**:
  - Enhanced touch targets for suggested search topic chips in `SettingsSearchResultsView` by enforcing a 36.dp minimum touch height, centered alignment, expanded internal padding (14.dp horizontal, 8.dp vertical), and 10.dp inter-chip spacing to eliminate unintended adjacent chip selection.
- **Contact Name Editing Persistence**:
  - Resolved an issue where editing contact details only persisted in-memory or in the local Room database, causing name modifications (e.g., from "Ryan" to "Ryan Shelby") to revert to the system contact name after app restart, process termination, or device reboot.
  - Implemented `updateContactInSystem` using atomic `ContentProviderOperation` batching to write name changes directly into Android's system `ContactsContract.Data` (`StructuredName.DISPLAY_NAME`, `GIVEN_NAME`, `FAMILY_NAME`), properly clearing obsolete family names and synchronizing phone numbers, emails, organization, notes, and photos.
  - Updated contact dashboard and list viewmodels to reload refreshed contact data directly from persistent storage upon saving.
- **Incoming Call Premature Silencing (Pocket & Inverted Orientation)**:
  - Fixed an issue where incoming calls were prematurely silenced when the device arrived in an upside-down orientation or inside a pocket.
  - Removed `Intent.ACTION_SCREEN_ON` from hardware key detection receiver so waking or turning on the screen upon call arrival does not silence the ringer.
  - Guarded `Intent.ACTION_SCREEN_OFF` to ignore screen transitions when the proximity sensor is covered (in pocket) or within the initial ringer grace period.
  - Added a 1200ms sensor stabilization window upon incoming call start in `CallGestureManager` to accurately capture initial pocket/face-down conditions without triggering gestures.
  - Restricted "Flip to Silence" to require confirmed active face-up viewing in open air for at least 400ms before a flip to face-down can silence the call. Calls arriving face-down or upside-down in a pocket will no longer silence the ringer.
  - Restricted "Proximity Wave to Silence" to require the device to be in open air for at least 600ms before a wave transition to near can trigger silence.

---

## [2.2.0] - 2026-09-14

### Added
- **Configurable SIM Selector Position (Middle vs Bottom)**:
  - Added a customizable display position for the "Select Calling SIM" popup, allowing users to choose between a centered floating dialog (**Middle**) with translucent frosted glass background and a traditional sliding **Bottom Sheet**.
  - Set **Middle** as the default display position across the app.
  - Added a dedicated "SIM Selector Position" card section in Settings under **Dual SIM Management** to seamlessly switch between Middle and Bottom modes.
  - Wired position preference reactively to both the Cellular Dial Pad and Recent History call-backs.
- **Recent Call History Filters (Incoming & Outgoing)**:
  - Added dedicated `Incoming` and `Outgoing` filter chips to the Recent screen filter bar.
  - Enables 1-tap filtering of call history logs across both daily date-grouped feeds and aggregated contact sessions.
- **Dial Pad "Always Ask" Multi-SIM Symbol**:
  - Added a dedicated Always Ask symbol icon (`?`) in the dial pad top-bar SIM selector pill when "Always Ask" mode is enabled in Settings.
  - Allows users to temporarily choose SIM 1 or SIM 2 directly from the dial pad before dialing, or remain on prompt-on-call without altering permanent settings.
- **Secret Dial Pad Fiery Call Button Easter Egg**:
  - Added a secret 3-second tap-and-hold interaction to the Cellular Dial Pad's Call button.
  - Features a slow-motion explosive detonation with expanding fire shockwave and shattered debris shards, followed by a magnetic vortex rewind and healing sequence that reforges the button into a permanent **Fiery Magma Style**.
  - Custom-designed animated fiery phone icon featuring 3-layered roaring flame wings (earpiece plumes, dragon-crest spine fins, rocket exhaust jets) and an incandescent molten white-gold core.
  - Real-time animated flame tip fluttering (`flameWobble`) and floating dancing ember sparks.
  - State persists in DataStore across app restarts and updates; holding for 3 seconds again triggers another slow-motion blast and healing sequence to restore the default Titanium Blue style.
  - Maintained complete secrecy with zero in-app UI or settings mentions; regular taps (<400ms) place calls instantly with zero latency.

### Changed
- **Dynamic SIM 2 Option Visibility (Single-SIM Optimization)**:
  - Dynamically hides the "SIM 2" selection row in **Dual SIM Management** settings when the device only has a single active SIM subscription.
  - Omits the dual-SIM selector pill on the Cellular Dial Pad on single-SIM setups, cleanly routing calls through the active SIM slot.
- **Dial Pad Default SIM Settings Affinity**:
  - The dial pad now strictly honors the user's default SIM preference from Settings ("SIM 1 Only", "SIM 2 Only", or "Always Ask") upon opening.
  - In-dialer SIM switching is strictly temporary and preserves the configured Settings default when the dial pad is reopened.

### Fixed
- **Bottom Sheet Dome Clipping & Header Truncation**:
  - Corrected `LineaShapes.extraLarge` from `CircleShape` to `RoundedCornerShape(28.dp)` to prevent Material 3 `ModalBottomSheet` and dialogs from inheriting a 50% semi-circle dome radius that clipped sheet titles ("Select Calling SIM", "Calling <number>") and action buttons.
  - Added explicit 28.dp rounded top corners and a top drag handle pill to `SimSelectSheet`, `AddEditCallRuleSheet`, `AddBlockSheet`, and `CallbackReminderSheet` with comfortable content padding.
- **Dial Pad SIM 2 Only Default Selection**:
  - Resolved issue where selecting "SIM 2 Only" in Settings still caused the dial pad to default to SIM 1.
- **Incoming Call Premature Ringer Silencing**:
  - Prevented Telecom's internal `onSilenceRinger()` setup invocation and early `VOLUME_CHANGED_ACTION` broadcasts from prematurely silencing the incoming ringtone before playback starts.
- **History Call-Back Dual-SIM Selection**:
  - Integrated `SimSelectSheet` prompt when initiating call-backs from Recent call logs with dual SIM and "Always Ask" enabled.
- **Quiet Hours Rule Initialization**:
  - Fixed database check to prevent duplicate Quiet Hours rule insertions.

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
