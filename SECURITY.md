# Security Policy

The LINEA project places utmost priority on user privacy, sovereign telephony, and data isolation. We are committed to maintaining a robust, auditable, and secure cellular communication application.

---

## 1. Supported Versions

Security patches and updates are actively maintained for the following versions:

| Version | Supported | Minimum OS Target | Recommended Target |
|---|---|---|---|
| **1.0.x** | :white_check_mark: Yes | Android 11 (API 30) | Android 17 (API 37) |
| **< 1.0.0** | :x: No | Deprecated | Deprecated |

---

## 2. Security Architecture & Core Safeguards

LINEA is engineered from the ground up with defensive security principles:

### 2.1. Zero Network & Zero Cloud Telemetry
- LINEA does not declare or use remote network capabilities for call routing, analytics, or feature flags.
- **Zero Third-Party SDKs**: No Google Firebase, Crashlytics, Mixpanel, Segment, or ad networks are bundled.
- All algorithms (T9 search, caller ID, availability insights, call screening, and DTMF tones) are 100% deterministic and execute purely on-device.

### 2.2. Hardware-Backed Encryption (AES-256-GCM)
- **Private Contacts Vault**: Sensitive contacts marked as "Private" are protected with 4-digit PIN authentication and AES-256-GCM encryption.
- Master cryptographic keys are generated and stored within Android's hardware-backed **AndroidKeyStore** (Secure Element / StrongBox when available).
- Private contacts are purged from the public contact index and isolated from third-party apps requesting `READ_CONTACTS`.

### 2.3. Native Telecom Boundary
- Calling operations strictly leverage Android’s native `TelecomManager`, `InCallService`, and `ConnectionService`.
- LINEA never intercepts audio streams without explicit user consent.
- Call recordings are stored in app-specific protected internal storage and marked with strict Unix file permissions.

---

## 3. Reporting a Vulnerability

We appreciate the efforts of security researchers and community contributors who practice responsible disclosure.

If you discover a security vulnerability or privacy flaw in LINEA:

1. **Do NOT open a public GitHub issue or pull request.**
2. Send a detailed report via email to our official security contact:
   - **Contact**: Ryan Shelby
   - **Email**: `ryn@disr.it`
   - **Subject**: `[SECURITY VULNERABILITY] LINEA - <Brief Description>`
3. Include the following details:
   - Affected version and build commit hash
   - Device model, Android OS version, and security patch level
   - Step-by-step reproduction instructions or Proof-of-Concept (PoC)
   - Impact assessment (e.g., local privilege escalation, cryptographic flaw, unauthorized data leak)

---

## 4. Response & Remediation Timeline

- **Initial Acknowledgment**: Within 48 hours of receiving the vulnerability report.
- **Triage & Validation**: Within 5 business days, confirming vulnerability scope and severity.
- **Remediation & Patch Release**: Critical security fixes will be prioritized, tested, and released promptly via public GitHub repository releases.
- **Public Disclosure**: Coordinated disclosure will occur only after a patch is published and available to users.

Thank you for helping keep LINEA and its users safe.
