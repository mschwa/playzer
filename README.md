# Playzer (Pure Jetpack Compose Skeleton)

A minimal Android app scaffold built entirely with Jetpack Compose (Material3), no legacy XML themes or view layouts. Focused on clarity and a clean foundation for a future music player.

## Key Characteristics
- Pure Compose UI: No XML layouts, theming defined in Kotlin (`PlayzerTheme`).
- Minimal but practical AndroidManifest (only app + launcher activity + icons + Application class).
- Material 3 custom color schemes (light/dark), typography, and shapes (in `ui/theme`).
- Runtime theme toggles (dark / dynamic color) wired straight into the Activity.
- Dynamic color support (Android 12+ automatically picks device palette).
- Instrumentation (Compose UI) test included.
- Unnecessary XML (themes, colors, strings, backup rules) removed/emptied.

## Module Overview
```
app/
  src/main/java/com/thorfio/playzer/
    MainActivity.kt          # Entry point with runtime theme toggles
    PlayzerApplication.kt    # Application-level hook (ready for DI/logging)
    ui/theme/                # Compose theme layer
      Color.kt
      Typography.kt
      Shape.kt
      Theme.kt
  src/androidTest/...        # Compose UI tests
  build.gradle.kts           # Compose + BOM + Material3 only
```

## Theming
`PlayzerTheme` selects:
- Dynamic color (if enabled & API >= 31)
- Otherwise static light/dark palettes (defined in `Color.kt`)
- Custom `PlayzerTypography` and `PlayzerShapes`.

Runtime toggles (in `MainActivity`):
- Dark mode (switch light/dark)
- Dynamic color (on/off, disabled on < API 31)

## Minimal Manifest Philosophy
Kept only what is useful:
- `android:label` for user-visible name
- Icons (so launcher displays branded icon)
- `android:allowBackup="false"` to prevent auto backup (adjust as you prefer)
- RTL support enabled
- Single launcher Activity (`MainActivity`)
- Custom `PlayzerApplication` for future initialization

No XML theme is referenced; window theming handled via Compose + system defaults.

## Dependencies (High-Level)
- Kotlin 2.0.x
- Android Gradle Plugin 8.13.x
- Compose BOM `2024.09.01`
- Material3, Navigation-Compose, Activity-Compose
- Compose foundation, animation, runtime-livedata
- UI tooling + test artifacts

## Build & Run (Windows)
```bat
REM Assemble debug APK
gradlew.bat :app:assembleDebug

REM Install & run on a connected device/emulator
gradlew.bat :app:installDebug
adb shell am start -n com.thorfio.playzer/.MainActivity
```

## Run Instrumentation (UI) Tests
Requires an emulator/device already running:
```bat
gradlew.bat :app:connectedAndroidTest
```
Relevant test: `PlayzerAppTest` validates greeting text and dark toggle behavior.

## Compose Preview
Open `MainActivity.kt` and use the two `@Preview` annotations (Light / Dark). Dynamic color preview requires running on a device/emulator to reflect actual palette; in previews you’ll see static palettes.

## Extending This Scaffold
Suggested next steps:
1. Navigation Graph: Introduce a root NavHost for multi-screen flows.
2. Media Playback Layer: Add ExoPlayer or Media3 libraries + a foreground service.
3. State Management: Introduce ViewModel + Flow/Coroutines for playback state.
4. Settings/DataStore: Persist user theme/dynamic preferences.
5. Dependency Injection: Wire Hilt or Koin once services appear.
6. UI Tests Expansion: Add semantics tags and more interaction tests.
7. Performance: Add baseline profile / macrobenchmark if startup critical.

## Design Notes
- AppCompat & Material (XML) removed; only Compose Material3 is used.
- Manifest intentionally does not specify a theme; if you want a custom splash or status bar control before first composition, create a super-light XML theme just for launch (optional).
- Colors currently static; you can generate tonal palettes dynamically later if you want adaptive theming beyond dynamic color.

## Troubleshooting
| Issue | Resolution |
|-------|-----------|
| Compose preview errors after sync | Invalidate caches / re-sync Gradle |
| Dynamic color buttons disabled | Device/emulator < Android 12 (API 31) |
| Want a splash screen | Add a launch theme + `android:exported` stays unchanged |
| Need localized app name | Reintroduce `strings.xml` and reference `@string/app_name` |

## License
Add your preferred license here (e.g., MIT / Apache 2.0) if you plan to open-source.

---
Feel free to request any of the “Extending This Scaffold” items and they can be added incrementally.

