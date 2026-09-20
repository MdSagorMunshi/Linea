# 🚀 LiNEA v2.3.0 — Release Notes

## Overview
**LiNEA v2.3.0** introduces bespoke liquid-capsule QR code contact sharing, an ultra-fast Google ML Kit scanner strictly filtered for LiNEA codes, one-tap number copying, and a compact 1-second hold-to-copy sheet for Recent call history.

---

### What's New in v2.3.0

#### 📷 Bespoke Liquid-Capsule QR Code Contact Sharing
- **Organic Liquid Matrix**: Designed an organic capsule matrix renderer where adjacent data modules fuse into continuous rounded capsules ($R = \text{moduleSize} \times 0.42$), eliminating harsh pixel squares.
- **Squircle Finder Eyes & Monogram Badge**: Precision squircle finder eyes with vibrant gradient centers and a central contact initial badge protected by QR Level Q error correction (25% data recovery).
- **5 Tactile Color Palettes**: Seamless transparent in-app previews and high-res export formats in Titanium Cyan, Emerald Mint, Cyber Amber, Neon Orchid, and Pure White.
- **Fast Access**: One-tap QR generator directly accessible from the Contacts list, Contact Profile Dashboard, and Recent Call Details.

#### 🔍 Strict LiNEA QR Code Scanner & Multi-Code Prioritization
- **Strict LiNEA Filtering**: Camera preview and gallery photo import engine strictly decode authentic LiNEA QR codes (`linea://contact` and `linea://`), ignoring random QR codes (websites, Wi-Fi setups, generic barcodes, plain text).
- **Multi-Code Prioritization**: When multiple QR codes are simultaneously visible in the camera frame or imported photo, the scanner isolates and detects only the LiNEA QR code.
- **Informative Validation**: Imported photos without valid LiNEA QR codes provide clear feedback without crashing or false-triggering.
- **Hardware Controls**: Tap-to-focus, flashlight torch toggle, and gallery photo picker with dual-pass ZXing fallback.

#### 📋 Instant One-Tap Number Copying
- **Decoded QR Card Copy Badge**: Added a prominent, frosted-glass `[ Copy ]` button to the decoded QR contact card alongside "Scan Again".
- **Per-Row Tap-to-Copy**: Tapping any phone number row in the decoded card copies it immediately to the clipboard with haptic feedback and a toast notification.

#### ⏱️ Recent Call Log 1-Second Hold-to-Copy Sheet
- **1-Second Touch & Hold**: Holding any call log entry on the **Recent** page (both Feed and Session modes) for 1 second triggers an ultra-compact bottom sheet.
- **Ultra-Compact Footprint**: Minimalist layout featuring the contact avatar and name horizontally aligned, followed by a sleek frosted-glass card displaying `PHONE NUMBER`, the full number, and a `[ ⎘ Copy ]` pill badge.
- **Direct Clipboard Action**: Tapping anywhere on the card or badge copies the number to the clipboard and smoothly dismisses the sheet.
- **Touch-Slop Safety**: Cancels hold detection if user is scrolling the list vertically or swiping horizontally for call-back / quick-action trays.

---

### Release Metadata
- **versionCode**: `5`
- **versionName**: `2.3.0`
- **Min SDK**: `Android 11 (API 30)`
- **Target SDK**: `Android 17 (API 37)`
