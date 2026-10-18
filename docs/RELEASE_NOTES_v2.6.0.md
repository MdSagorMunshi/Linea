# 🚀 LiNEA v2.6.0 — Release Notes

## Overview
**LiNEA v2.6.0** introduces the **Navbar Customization System** — allowing full visual, dimensional, and animation control over the bottom navigation bar — alongside critical **Incoming Call Ringtone Resilience** enhancements, AAC-LC asset transcoding, and extended Telecom startup grace periods.

---

### What's New in v2.6.0

#### 🧭 Navbar Customization System
- **Style Selector**: Choose between:
  - **Floating Circle (Default)**: A minimalist, floating circular glass button with 1-tap expansion, 5-second automatic collapse, and direct 4-way gesture flick navigation.
  - **Expanded Pill Bar**: A persistent, full-width frosted glass pill bar with instant direct one-tap tab switches.
- **Dynamic Circle Sizing**: Customize the physical diameter and thumb touch footprint of the collapsed floating button across 4 distinct sizes:
  - **Compact**: `48 dp` (minimal visual footprint, ideal for small screens).
  - **Standard**: `56 dp` (default balanced ergonomic sizing).
  - **Comfortable**: `64 dp` (generous touch targets for easy reach).
  - **Large**: `72 dp` (maximum accessibility and thumb reachability).
- **Animation & Motion Dynamics**: Configure morphing and stretch spring dynamics with selectable physics profiles:
  - **Bouncy Spring (Default)**: Playful spring bounce with high damping ratio.
  - **Smooth Fluid**: Silky smooth motion with low stiffness and critical damping.
  - **Snappy Quick**: High-stiffness instant transitions with minimal bounce.
  - **Minimal Fade**: Clean, subtle crossfades for low-distraction navigation.
- **Interactive Live Sandbox**: An embedded live preview inside Settings allowing users to interact with, expand, and flick the actual navbar while tweaking styles, sizes, and physics in real time.
- **Gesture Quick-Reference Guide**: Interactive visual guide detailing Tap expansion, 4-Way Gesture Flick (jump directly to tabs without expanding), and Triple-Tap persistence toggle.
- **Settings Integration & Search Engine**: Dedicated entry under **Appearance & Motion** with dynamic status subtitles, mode badges, and instant discovery via the Settings search engine.

---

### Key Fixes & Optimizations in v2.6.0

#### 🔊 Ringtone Resilience & Codec Transcoding
- **Standard AAC-LC Transcoding**: Re-encoded embedded ringtones (`ring_glass.m4a`, `ring_minimal.m4a`) to standard AAC-LC / 44.1kHz stereo format, eliminating playback stalls and decoding failures on strict hardware media decoders.
- **3-Tier Ringer Fallback Chain**: Built resilient fallback architecture in `IncomingCallActivity` attempting custom raw resource &rarr; system ringtone URI stream &rarr; default notification alert sound, guaranteeing an incoming call is never silent even if a custom sound resource fails to load.
- **Telecom Silence Grace Period Extension**: Extended startup silence grace period from 600ms to 2000ms upon initial Telecom connection to eliminate ringer startup race conditions and prevent spurious volume key events from silencing the incoming call ringtone.

---

### Release Metadata
- **versionCode**: `10`
- **versionName**: `2.6.0`
- **Min SDK**: `Android 11 (API 30)`
- **Target SDK**: `Android 17 (API 37)`

---

### Assets
| Asset Name | SHA-256 Digest | Size |
|---|---|---|
| `LiNEA-v2.6.0-release.apk` | `314acccb4f82ef64ee2378d80c9efdb776454c86ae2d0e39f6c818e79c3c1958` | 30.08 MiB |

