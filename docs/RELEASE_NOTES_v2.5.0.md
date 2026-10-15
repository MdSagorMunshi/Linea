# 🚀 LiNEA v2.5.0 — Release Notes

## Overview
**LiNEA v2.5.0** is a major stable release introducing the **Country-Based Call Blocking System**, an overhaul of the incoming call audio pipeline and telecom logging, battery drain optimizations, native default dialer power exemption support, and missed call notification deduplication.

---

### What's New in v2.5.0

#### 🌍 Country-Based Call Blocking System
- **Comprehensive Country Filter**: Blacklist or whitelist international callers by country with strict "Whitelist Only" and "Blacklist Only" filtering modes.
- **High-Precision Country Detection**: Powered by Google `libphonenumber` to accurately identify caller countries from E.164 and localized numbers, seamlessly disambiguating shared country codes (e.g., USA vs. Canada under `+1`, Russia vs. Kazakhstan under `+7`).
- **Granular Exemptions**: Specific countries under a shared dialing code can be allowed even if the code itself is blocked.
- **Domestic & Contact Bypass**: Contacts and calls originating from the device's domestic SIM country bypass blocking automatically to ensure critical calls are never missed.
- **Management UI**: Dedicated settings interface with flag-based selectors, search integration, and a status dashboard on the main Blocking Screen.

---

### Key Fixes & Optimizations in v2.5.0

#### 🔊 Incoming Call Audio Pipeline & Ringer
- **Audio Attributes & Stream**: Configured ringtone playback strictly with `USAGE_NOTIFICATION_RINGTONE` and `STREAM_RING`, properly requesting transient audio focus with ducking.
- **Eliminated Silent Ringing**: Resolved volume key silencing race conditions and state desynchronization that previously caused incoming calls to ring silently.
- **Vibration Synchronization**: Ensured vibration patterns synchronize cleanly with ringtone playback and immediately halt upon call answer, reject, or disconnect.

#### 🔋 Battery Drain Elimination & Power Exemption
- **Native Default Dialer Exemption**: Fully integrated Android's native `ROLE_DIALER` unrestricted battery usage exemption (`PowerExemptionManager.REASON_ROLE_DIALER`), eliminating redundant "Background Execution ATTENTION" setup warnings.
- **NFC Radio Transmitter Scoping**: Removed continuous background reader mode polling from `MainActivity.onResume()`. Scoped NFC hardware polling strictly to `LineaBeamSheet` via `DisposableEffect`, eliminating continuous 2–3Hz radio transmitter pulsing.
- **Sensor Hardware Leak Prevention**: Synchronously cancelled pending startup coroutines upon `stopListening()` in `CallGestureManager`, ensuring accelerometer and proximity sensors never remain active in the background after quick call termination.
- **Proximity Sensor Power Rail Release**: Changed proximity wake lock release to drop immediately (`release(0)`) upon call disconnect, preventing the physical proximity sensor rail from holding power for up to 2 hours when placed face-down or pocketed.
- **Worker Constraints**: Added `setRequiresDeviceIdle(true)` and `setRequiresBatteryNotLow(true)` to periodic call history maintenance work.

#### 🔔 Missed Call Notification Deduplication
- **Phone Number Digit Normalization**: Normalized digits so raw telecom handles and formatted numbers map to identical notification IDs.
- **Suppression Window**: Added a 15-second deduplication suppression window in `CallNotificationManager` to prevent duplicate alerts.
- **Broadcast Coordination**: Updated `MissedCallReceiver` to suppress redundant platform `ACTION_SHOW_MISSED_CALLS_NOTIFICATION` broadcasts when `InCallService` has already posted, while maintaining asynchronous contact lookup fallback.

#### 📋 Telecom Logging & Call History
- **Accurate Duration Tracking**: Computed call duration using elapsed monotonic connect time rather than wall-clock differences, preventing duration errors during screen-off sleep.
- **Direction Classification**: Fixed direction assignment on call removal to ensure missed, incoming, and outgoing calls are categorized and persisted to Room without dropped records.

---

### Release Metadata
- **versionCode**: `9`
- **versionName**: `2.5.0`
- **Min SDK**: `Android 11 (API 30)`
- **Target SDK**: `Android 17 (API 37)`
