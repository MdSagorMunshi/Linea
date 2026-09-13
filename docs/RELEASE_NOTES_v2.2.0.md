# 🚀 LiNEA v2.2.0 — Release Notes

## Overview
**LiNEA v2.2.0** brings enhanced dual-SIM management, smart call history filtering, critical telephony audio fixes, and a secret cinematic Easter Egg.

---

### What's New in v2.2.0

#### 📱 Configurable SIM Selector Position (Middle vs Bottom)
- **Floating Middle Dialog (Default)**: The "Select Calling SIM" overlay now floats gracefully in the center of the screen within a translucent frosted glass neumorphic card with backdrop blur and tap-outside dismissal.
- **Traditional Bottom Sheet Option**: Users preferring a sliding sheet from the bottom can toggle it with a single tap in **Settings → Dual SIM Management**.
- **Unified Across App**: Both the Cellular Dial Pad and Recent History call-backs honor this position preference reactively.

#### 🔍 Recent Call History Smart Filters (Incoming & Outgoing)
- Added dedicated `Incoming` and `Outgoing` filter chips to the Recent screen filter bar alongside `All` and `Missed`.
- Filter logs instantly across daily grouped feeds and aggregated contact sessions without re-querying the database.

#### 🔢 Dial Pad "Always Ask" Symbol & Settings Affinity
- **Always Ask Symbol (`?`)**: Added a clean `?` icon in the dial pad top-bar SIM pill when "Always Ask" mode is enabled in Settings, preserving neumorphic layout integrity.
- **Settings Affinity**: The dial pad strictly honors the user's configured default SIM ("SIM 1 Only", "SIM 2 Only", or "Always Ask") upon opening. In-dialer SIM switching is temporary and never inadvertently overwrites permanent settings.

#### 📲 Dynamic Single-SIM UX Optimization
- Devices with only one active SIM slot automatically hide the static "SIM 2" row under Dual SIM settings.
- The dial pad SIM selector pill is omitted on single-SIM setups, cleanly routing calls through the active SIM.

#### 🛡️ Telephony Audio & UI Fixes
- **Bottom Sheet Dome Clipping**: Corrected `LineaShapes.extraLarge` from `CircleShape` to `RoundedCornerShape(28.dp)`, eliminating severe 50% dome clipping on sheet headers and buttons across `SimSelectSheet`, `AddBlockSheet`, `AddEditCallRuleSheet`, and `CallbackReminderSheet`.
- **Incoming Call Ringer Silencing**: Prevented Telecom's internal `onSilenceRinger()` setup invocation and early `VOLUME_CHANGED_ACTION` broadcasts from prematurely silencing the incoming ringtone before playback starts.
- **History Call-Back Dual-SIM Selection**: Integrated `SimSelectSheet` prompt when initiating call-backs from Recent call logs with dual SIM and "Always Ask" enabled.
- **Dial Pad SIM 2 Only Default**: Resolved an issue where selecting "SIM 2 Only" in Settings still caused the dial pad to default to SIM 1.

#### 🔥 Secret Easter Egg: Animated Fiery Magma Call Button
- **3-Second Hold Trigger**: Tap and hold the main Call button on the Cellular Dial Pad for 3 seconds to detonate an explosive slow-motion blast with fire shockwaves, embers, and shattered button shards.
- **Slow-Motion Healing / Rearranging**: Following peak dispersion, shards magnetically rewind and reassemble into a permanent **Fiery Magma Style**.
- **Custom-Designed Animated Flame Icon**: Hand-crafted fire handset featuring 3-layered roaring flame plumes, dragon-wing spine crests, rocket exhaust jets, and an incandescent white-gold core line with real-time fluttering flame tips (`flameWobble`) and floating ember sparks.
- **Reversible & Persistent**: Saved in DataStore across app restarts; holding for 3 seconds again reverses the button back to default Titanium Blue.
- **Zero In-App Mentions**: Kept entirely secret; normal short taps (<400ms) continue to place calls immediately.

---

### Release Metadata
- **versionCode**: `4`
- **versionName**: `2.2.0`
- **Min SDK**: `Android 11 (API 30)`
- **Target SDK**: `Android 17 (API 37)`
