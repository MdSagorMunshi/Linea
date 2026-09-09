# Architecture & Engineering Specifications

This document outlines the system architecture, component boundaries, telephony pipelines, and data flow of the LINEA dialer application.

---

## 1. High-Level System Architecture

LINEA is structured according to **Clean Architecture** and **Unidirectional Data Flow (UDF)** patterns, organized across three distinct operational layers:

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│    Jetpack Compose UI • ViewModels • Navigation Graph       │
│    (LineaTheme, FrostedGlassBox, Scrubber, Tabular Figures) │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow / Events
┌──────────────────────────────▼──────────────────────────────┐
│                      TELECOM & DOMAIN                       │
│  InCallService • ConnectionService • PhoneAccountManager    │
│  CallManager • AudioSwitch • CallScreeningEngine • T9Search │
└──────────────────────────────┬──────────────────────────────┘
                               │ Coroutine / Dispatchers.IO
┌──────────────────────────────▼──────────────────────────────┐
│                    DATA & PERSISTENCE                       │
│  Room DB (11 Relational Entities) • DataStore Preferences   │
│  ContactsContract ContentResolver Sync • Encrypted Storage  │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Core Subsystems

### 2.1. Telephony & Native Telecom Framework
- **`LineaInCallService`**: Extends Android's native `android.telecom.InCallService`. Binds to system calls when LINEA is the designated default dialer. Receives lifecycle callbacks (`onCallAdded`, `onCallRemoved`) and tracks call state (`STATE_DIALING`, `STATE_RINGING`, `STATE_ACTIVE`, `STATE_HOLDING`, `STATE_DISCONNECTED`).
- **`CallManager`**: Singleton coordinator managing active calls, DTMF audio generation (`playDtmfTone`), audio routing (`ROUTE_EARPIECE`, `ROUTE_SPEAKER`, `ROUTE_BLUETOOTH`), conference merging, call hold/swap states, and gesture coordination.
- **`CallGestureManager`**: Hardware sensor listener monitoring device orientation (`Sensor.TYPE_GRAVITY` / `Sensor.TYPE_ACCELEROMETER`) and proximity (`Sensor.TYPE_PROXIMITY`). Implements:
  - *Flip to Silence*: Detects when the device is inverted face-down (`Z < -8.0 m/s²`) and triggers ringer muting with haptic confirmation.
  - *Proximity Wave*: Detects hand passing over the front proximity sensor (`distance < threshold` followed by release) to silence the ringer touch-free.
  - *Zero-Idle Lifecycle*: Strictly binds listeners only when `CallManager` enters `LineaCallState.RINGING` and unbinds immediately upon answering, declining, or silencing.
- **`PhoneAccountManager`**: Inspects `TelecomManager` and `SubscriptionManager` to register system `PhoneAccountHandle` instances for SIM 1 and SIM 2, enabling hardware-level dual SIM routing and live signal telemetry.
- **`LineaConnectionService`**: Implements system connection protocols for carrier-grade telephony negotiation and MMI/USSD command delivery.

### 2.2. Deterministic Calling & Screening Engine
- **`CallScreeningEngine`**: Handles incoming call events via `CallScreeningService`. Performs instantaneous, zero-latency rule matching against:
  - Exact phone numbers (E.164 normalized)
  - Prefix wildcards (e.g. `+1800*`)
  - International caller filters
  - Quiet hours schedules with day-of-week recurrence
- **Emergency Repeated-Call Tracker**: If an unknown or blocked caller attempts 3 calls within a 5-minute window, the engine automatically triggers an emergency bypass to ensure critical calls are never missed.

### 2.3. T9 Smart Dialpad Search
- Built on a precomputed on-device reverse prefix index.
- Numbers `2`–`9` map deterministically to standard telephone keypad characters:
  - `2` &rarr; `ABC`, `3` &rarr; `DEF`, `4` &rarr; `GHI`, `5` &rarr; `JKL`, `6` &rarr; `MNO`, `7` &rarr; `PQRS`, `8` &rarr; `TUV`, `9` &rarr; `WXYZ`
- Search queries perform concurrent bipartite matching:
  - Number prefix match (matches dial strings directly)
  - Name prefix match (matches first name, last name, and company tokens)
- Zero allocation during active typing to maintain continuous 120Hz frame rates.

### 2.4. International Country & Time Zone Engine
- **`InternationalCountryHelper`**: Deterministic country code resolver supporting 40+ global dial prefixes.
- Computes destination local time via `java.time.ZonedDateTime` and detects late-night calling conditions (10:00 PM – 6:00 AM) to prevent accidental disruptive calls across time zones.

### 2.5. Data Storage & Schema Architecture
The persistence layer is powered by **Room 2.6.1** over SQLite (`linea_database.db`):

| Entity | Table Name | Purpose |
|---|---|---|
| `ContactEntity` | `contacts` | Primary contact record (name, company, photo URI, favorite, pinned, private) |
| `ContactNumberEntity` | `contact_numbers` | Relational phone numbers linked by foreign key cascade to `contacts.id` |
| `ContactEmailEntity` | `contact_emails` | Relational email addresses linked by foreign key cascade to `contacts.id` |
| `ContactGroupEntity` | `contact_groups` | Custom categorization groups (e.g. Work, Family) |
| `ContactGroupMemberEntity` | `contact_group_members` | Many-to-many junction table linking contacts and groups |
| `CallRecordEntity` | `call_records` | Coalesced call logs, durations, SIM slot ID, and call type |
| `CallRuleEntity` | `call_rules` | Screening rules, quiet hour intervals, and allow/block lists |
| `CallRecordingEntity` | `call_recordings` | Metadata, audio file path, duration, and waveform data |
| `CallNoteEntity` | `call_notes` | Contextual post-call and pre-call pop notes |
| `BlockedNumberEntity` | `blocked_numbers` | Dedicated fast-lookup blacklist for instant rejection |
| `DialerProfileEntity` | `dialer_profiles` | User profiles configuring SIM affinity, ringtones, and vibration |

---

## 3. UI/UX Design System: Industrial Glassmorphism

LINEA does not use standard Material palettes. All composables utilize custom design primitives defined in `ui/theme/` and `ui/incall/components/`:

- **Frosted Surfaces**: `FrostedGlassBox` overlays a subtle translucent surface (`Color.White.copy(alpha = 0.08f)`) with a hairline titanium border (`Color.White.copy(alpha = 0.12f)`).
- **Tabular Figures**: All durations, phone numbers, and time counters explicitly use `FontFeature("tnum")` to prevent horizontal jitter during active call duration increments.
- **Dynamic Glows & Ring Auras**: Dialing and incoming call states render multi-ring radial expanding pulses reflecting caller state.
- **Live Audio Waveform**: Multi-harmonic 32-bar fluid liquid glass visualizer (`LiveAudioWaveform`) reacting in real time to speech, mute flatlines, holding pulses, and recording glows.
- **Full-Screen Frosted Contact Posters**: Gaussian photo blur backdrop (`ContactPosterBackground`) with subtle breathing scale animations, backed by a 16MB in-memory LRU cache.
- **Tactile Call Controls**: Custom circular glass action buttons with spring micro-press physics and glowing state borders.
- **Quick Decline Action Sheet**: Swipe-up frosted glass bottom sheet (`QuickDeclineSheet`) with 1-tap quick SMS preset chips and direct custom message input.
- **Haptic Audio Dialpad Profiles**: Selectable acoustic and tactile profiles (`Titanium Glass`, `Mechanical Relay`, `Stealth`, `Classic`) adjusting both DTMF tone parameters and vibration effect primitives.

