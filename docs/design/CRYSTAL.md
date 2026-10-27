# LiNEA Crystal prototype

Choose **Settings → Appearance & Motion → Design → Crystal**. Classic remains the
fallback for existing installations and unknown preference values. The design
choice is saved separately from Light, Dark, OLED, and System color mode.

## Included

- Pearl and graphite palettes, with a black-background OLED variant. Crystal
  replaces legacy neon accent tokens with muted steel, sage, and neutral tones.
- Shared glass surfaces: translucent fills, directional reflections, beveled
  borders, elevation shadows, and a stationary background light field.
- Rounded glass dialpad keys with spring compression and a subtle perspective
  tilt. Existing digit entry, tones, haptics, and number-key long presses remain.
- A glass call button whose long press reveals an alternate platinum skin.
  The reveal does not invoke the call action. Classic retains its fiery Easter egg.
- A floating dock with a moving selected-tab lens and accessible tab semantics.
  Classic's saved circle/pill navigation settings remain available when using Classic.
- Recents cards with inline expansion and a separate “View call details” action.
  Foreground cards conceal swipe-action trays; the dock conceals scrolling text.
- Appearance previews, settings search integration, and wrapping color-mode controls.
- Reduced-motion handling for new press, skin, dock, and expansion animations.

## Implementation boundary

The existing view models and calling flows are shared by both designs. This first
prototype focuses on Dialpad, Recents, Appearance, and navigation. Other screens
inherit the palette and shared surfaces, but do not yet have bespoke Crystal layouts.

The glass material uses Compose gradients, translucency, and shadows; it does not
perform live background blur, optical refraction, or render a 3D scene. Depth is
created with perspective transforms and layered surfaces. This avoids continuous
background capture and idle animation. Device performance still needs measurement
before claiming any specific frame-rate target.

## Validation

Run:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest \
  --tests com.ryanshelby.linea.CrystalDesignTest \
  --tests com.ryanshelby.linea.SettingsSearchEngineTest \
  --tests com.ryanshelby.linea.FieryCallButtonTest
```

`CrystalDesignTest` covers safe preference fallback, independent light/dark/OLED
selection, and minimum 4.5:1 text contrast on conservative translucent backgrounds.
The call-button unit tests predate Crystal and cover preference behavior; gesture
verification is performed on the emulator.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Emulator checks completed on Android at 1440 × 3120 / 560 dpi, plus a 360 dp-wide
configuration with font scale 1.3. Verified light/dark rendering, inline Recents
expansion, entering digits, long-press skin switching without dialing, restoration
after process restart, and switching back to Classic. Emulator density and font
scale were restored after testing. The final APK is installed on the emulator.
