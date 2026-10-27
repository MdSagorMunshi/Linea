# LiNEA Crystal design

Choose **Settings → Appearance → Design → Crystal**. Classic remains available.
The saved design is independent of Light, Dark, OLED, and System color mode.

## Screens and interaction

Crystal includes a dedicated dialpad composition, People and contact profiles,
Recents with inline expansion, a profile-first Settings screen with expandable
Appearance controls, and incoming/active call screens. Shared typography, shapes,
form controls, panels, sheets, and motion keep the remaining screens consistent.

- Pearl and graphite materials with a black-background OLED variant; restrained
  steel, sage, and brick accents. Legacy neon tokens are neutralized in Crystal.
- The dialpad keeps number entry and suggestions above a stable keypad and call
  action. This upper region can scroll on compact screens when suggestions or
  setup banners need more room.
- Glass keys compress and tilt on press. Holding Call reveals its alternate skin
  without invoking the call action. Number-key speed dial and “hold 0 for +” remain.
- The floating dock refracts and diffuses the content scrolling beneath it. Its
  selected-tab lens travels with spring motion. Screen changes use short spatial
  transitions; reduced motion uses fast fades and disables perspective effects.
- People has a portrait-focused favorites strip, larger contact rows, and an
  alphabet rail with clearance above the dock. Profiles retain call/message/share,
  editing, account selection, and private-contact behavior.
- Call controls sample the caller's own poster. Their text and icons remain sharp,
  and colored actions use contrasting foregrounds in both color modes.

## Live glass renderer

`CrystalBackdropCapture.kt` records a GPU `GraphicsLayer` for the material source.
Controls sample the decorative background. The dock uses a separate capture of
screen content, and call screens capture their contact poster. Keeping source and
effect layers separate prevents recursive self-capture.

On hardware-accelerated Android 13+, an AGSL `RuntimeShader` uses a rounded shape's
signed distance and edge normal to displace actual source pixels, with directional
highlights. `RenderEffect` chains this refraction with backdrop blur. Android 12
and 12L use backdrop blur. Android 11, software rendering, and separate dialog
windows without a source use readable opaque materials. The glass renderer uses no colored
chromatic dispersion, screenshot readback, added rendering dependency, or idle
animation loop.

See Android's [graphics layer documentation](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers)
and [AGSL guide](https://developer.android.com/develop/ui/views/graphics/agsl/using-agsl).

## Validation

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest \
  --tests com.ryanshelby.linea.CrystalDesignTest \
  --tests com.ryanshelby.linea.SettingsSearchEngineTest \
  --tests com.ryanshelby.linea.FieryCallButtonTest
```

The design tests cover preference fallback, independent light/dark/OLED selection,
text contrast over conservative translucent backgrounds, and the Android-version,
software-rendering, and missing-source fallback policy. Gesture behavior is checked
on the emulator; existing call-button unit tests cover preference behavior.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
Optimized signed APK: `app/build/outputs/apk/release/app-release.apk`.

## Verification record

The debug build and 18 focused unit tests pass. Android lint reports 15 existing
errors in files unchanged by this design update (telecom permissions, activity key
dispatch, and the manifest hardware declaration); it reports no errors in the new
glass renderer or design code.

Emulator checks cover light/dark appearance, number entry, long-press skin change
without dialing, contact profiles, and a 360 dp viewport at 1.3 font scale. The
built-in call simulator verifies incoming, Answer, active call controls, and End
Call without a carrier connection. Display settings are restored after testing.
