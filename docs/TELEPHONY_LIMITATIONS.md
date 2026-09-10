# Cellular SIM selection and recording

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

## Cellular call recording

This app has a minimum SDK of Android 11. On Android 10 and newer, an ordinary
third-party app cannot capture cellular call uplink/downlink audio. Android
requires privileged `CAPTURE_AUDIO_OUTPUT` access for this. `RECORD_AUDIO` and
`VOICE_COMMUNICATION` alone only request microphone/communications input and
can return silence while Telecom owns the active call.

Linea attempts the proper `VOICE_CALL` source for both automatic and manual
recording whenever microphone permission is granted. This supports OEM builds
that authorize their default dialer without exposing the privileged permission.
It checks for a real audio signal and deletes a silent output rather than saving
it as a recording. On devices that enforce the standard Android policy, Linea
reports that call audio was blocked. OEM policy and local recording-consent laws
still apply.

## Hardware validation checklist

Use a physical dual-SIM device with Linea set as the default dialer:

1. Set SIM 1 as Android's default calling SIM, then place a call after selecting
   SIM 2 in Linea; verify the carrier/network indicator and call log show SIM 2.
2. Repeat in reverse, and repeat after disabling/removing one SIM. A missing SIM
   must not be silently substituted for the default SIM.
3. For recording, test outgoing and incoming calls on both SIMs, auto-record and
   manual record. On an ordinary Android 10+ build, verify the explanatory
   message and that no blank file is created. On a privileged/OEM-authorized
   build, verify both directions contain audible audio.
