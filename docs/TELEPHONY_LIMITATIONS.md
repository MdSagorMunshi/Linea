# Cellular SIM selection and Hardware Telephony Architecture

## SIM selection

Linea enumerates active subscriptions and maps each one to its enabled
`PhoneAccountHandle` with `TelephonyManager.getSubscriptionId(handle)`. When a
user chooses a SIM, Linea passes that exact handle to
`TelecomManager.placeCall()` using `TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE`.
It does not derive a subscription from the order of phone accounts, and it does
not reuse the device default account for a different SIM.

An active subscription for which Android does not expose an enabled,
subscription-matched phone account is not presented as a selectable calling
SIM. If Telecom subsequently reports a different account for an explicitly
selected outgoing call, Linea disconnects it rather than continuing it on the
system default account.

## Cellular call recording (Removed in v2.1.0)

LINEA targets Android 17 with a minimum SDK of Android 11 (API 30). On Android 10 and newer,
ordinary userland dialers cannot reliably capture bidirectional cellular call uplink/downlink audio
without system-privileged `CAPTURE_AUDIO_OUTPUT` permission or specialized OEM firmware hooks.
AOSP `AudioPolicyService` enforces strict audio isolation while Android Telecom owns the cellular stream,
returning zero-amplitude silence or conflicting with active earpiece routing.

Consequently, **call recording and real-time audio waving were permanently removed in v2.1.0**.
LINEA maintains an uncompromised focus on sovereign, zero-latency, private, and dependable cellular communications.
Legacy Room persistence schemas (`call_recordings`) remain dormant to guarantee non-destructive database migrations for existing installations.

## Hardware validation checklist

Use a physical dual-SIM device with Linea set as the default dialer:

1. **Dual-SIM Routing**: Set SIM 1 as Android's default calling SIM, then place a call after selecting
   SIM 2 in Linea; verify the carrier/network indicator and call log show SIM 2.
2. **SIM Fault Tolerance**: Repeat in reverse, and repeat after disabling/removing one SIM. A missing SIM
   must not be silently substituted for the default SIM.
3. **Volume Button Silencing**: During incoming ringing, press Volume Up or Volume Down once. Verify ringtone
   and vibration instantly stop while the call continues to ring silently and remains answerable.
4. **Double-Press Power to End**: During incoming ringing, outgoing dialing, or an active conversation,
   rapidly double-press the hardware Power button. Verify the call is terminated immediately with haptic confirmation.
