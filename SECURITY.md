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

### 2.2. Hardware-Backed & Quantum-Resistant Encryption (AES-256-GCM & Argon2id)
- **Private Contacts Safe & Vault**: Sensitive contacts marked as "Private" are protected with 6-digit PIN authentication, Argon2id memory-hard hashing, and AES-256-GCM enveloped master key encryption.
- **Symmetric Quantum Resistance**: 256-bit symmetric keys provide 128 bits of post-quantum security against Grover's algorithm, satisfying NIST Post-Quantum Cryptography Level 5 requirements.
- **Argon2id Password & Recovery Hashing (RFC 9106)**: PIN and security recovery credentials utilize memory-hard Argon2id hashing (19 MiB RAM matrix, 3 iterations, 32-byte cryptographically secure random salt) protecting against GPU, ASIC, and quantum-circuit brute force.
- **Hardened Recovery Architecture**: Security answers strictly require a minimum of 9 characters and are never stored in plaintext or reversible formats. PIN resets require cryptographically bound, ephemeral single-use recovery sessions, preventing client-side bypasses and unauthorized resets.
- **Multi-Tier Anti-Brute-Force Lockout**: Enforces progressive exponential lockouts (30s at 5 attempts, 2m at 8 attempts, 10m at 10+ attempts) and constant-time comparisons (`MessageDigest.isEqual`) across both PIN unlock and recovery verification.
- **Local Isolation & Memory Protection**: Master keys are zeroized from RAM upon vault lock. Private contacts are completely isolated to local encrypted app storage and removed from the public contact provider.

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
