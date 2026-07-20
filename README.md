# Cloudy - LumiROM OTA updater (Samsung Galaxy A32 4G, A-only)

OneUI 8 (SESL8) OTA updater for the LumiROM custom ROM. Package `com.aerocat.cloudy`, v8.6.4 Beta.

## UI stack: OneUI 8 / SESL8 (tribalfs)
Dependencies come from the version catalog (`gradle/libs.versions.toml`) as `libs.bundles.sesl`.
The SESL fork REPLACES upstream `androidx.appcompat` / `material` / `core` / `fragment` (same package
names, so Kotlin code is unchanged). Verified coordinates and mutually-compatible sesl8 versions are
taken from the oneui-design library's own catalog.

### Authentication (required)
SESL8 is published on GitHub Packages (Maven) and needs auth even though it's public:
- Create a GitHub PAT (classic) with the `read:packages` scope.
- Local builds: put in `~/.gradle/gradle.properties`:
  ```
  gpr.user=your_github_username
  gpr.key=ghp_yourToken
  ```
- CI (GitHub Actions): add repo secrets `GPR_USER` and `GPR_TOKEN`; the workflow exposes them as
  env vars that `settings.gradle.kts` reads. The automatic `GITHUB_TOKEN` will NOT work - it can't
  read another account's packages.

### OneUI dynamic color
`AndroidManifest.xml` declares `theming-meta` + `theming-meta-xml` pointing at
`res/xml/theming_meta_cloudy.xml`, and uses `android:theme="@style/OneUITheme"` (provided by the lib -
do NOT redefine it). This is what applies the device's OneUI color palette.

## OTA architecture
- `data/` - JSON model + fetch/download with SHA-256 verify.
- `ota/` - `DeviceInfo`, `RootManager` + persistent root worker (`RootService` + `IRootIpc`/`IFlashCallback` AIDL),
  `OtaInstaller` (privileged -> rooted recovery-staging -> gated raw block), `VersionCheck` (ro.cloudy.version).
- `ui/` - `MainActivity` (ToolbarLayout + TabLayout + ViewPager2), Check Update / Maintainer / Settings.

## ROM props Cloudy reads
Bake these into the LumiROM build props:
```
ro.cloudy.rom.ver=8.6.4          # ROM version (shown as "Installed version")
ro.cloudy.rom.ver.code=80604     # optional numeric, preferred for comparison
ro.cloudy.maintainer=aerocat     # shown on the Maintainer tab
```
Update detection order: `ro.cloudy.rom.ver.code` vs manifest `version_code` (int) ->
`ro.cloudy.rom.ver` semver -> build-fingerprint fallback.

## Default OTA manifest URL
```
https://raw.githubusercontent.com/cloudyota/ota-update/16.2/<codename>.json
```
`<codename>` is auto-detected from `ro.product.device` (fallback `Build.DEVICE`), e.g. `a32.json`.
Overridable in Settings -> Custom JSON URL.

## Fonts and corner radius (important)
- **One UI Sans**: proprietary Samsung font, cannot be bundled. The app forces
  `android:fontFamily="sec"` (the same alias oneui-design uses internally), which resolves to
  One UI Sans on One UI 8 devices and falls back to the platform sans elsewhere.
- **Corner radius**: SESL's RoundedLinearLayout inherits the *system* corner radius, which on
  non-Samsung devices (e.g. the Pixel used by Firebase Robo tests) falls back to a small AOSP
  value and looks like One UI 4. Cards therefore use `@drawable/bg_oui_card` (26dp) so the
  One UI 8.5 radius is identical on every device.

## Build
```
./gradlew clean assembleDebug
```
CI: `.github/workflows/build.yml` (needs the GPR_USER / GPR_TOKEN secrets above).
Magisk module: `cd magisk-module && zip -r ../cloudy_ota.zip .` then flash in Magisk/KernelSU.

## Verify against the sample app
A few OneUI 8 specifics can drift between releases - if a widget or theme attr fails to resolve,
cross-check the oneui-design sample app (https://github.com/tribalfs/oneui-design/tree/main/sample-app).
Rows here use standard TextViews (guaranteed to compile) rather than lib-specific list widgets;
swap in OneUI list/preference widgets if you want the fuller native look.

## Honest caveats
- Settings-dashboard injection indexes only when Cloudy is a privileged/system app in the ROM.
- Non-root can't flash on modern Samsung; tier 1 needs priv-app, tiers 2-3 need root + the Cloudy module.
- A-only direct flashing is brick-prone; the safe default hands the package to recovery.
