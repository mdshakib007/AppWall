<p align="center">
  <img src="app/src/main/res/drawable-nodpi/logo.webp" width="110" alt="AppWall">
</p>

<h1 align="center">AppWall</h1>

<p align="center">
  <b>App &amp; website blocker for Android.</b><br>
  Free · Open source · No server · No account · No tracking
</p>

<p align="center">
  <a href="https://github.com/mdshakib007/AppWall/actions/workflows/build.yml"><img src="https://github.com/mdshakib007/AppWall/actions/workflows/build.yml/badge.svg" alt="Build"></a>
  <a href="https://github.com/mdshakib007/AppWall/releases/latest"><img src="https://img.shields.io/github/v/release/mdshakib007/AppWall?label=download" alt="Latest release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-orange" alt="MIT"></a>
</p>

AppWall keeps you away from the apps and websites that eat your time. Block anything forever, for a few hours, until a date, or on a daily schedule. When you mean it, turn on **Focus Mode** and lock your whole list for days or weeks with no way back.

Everything happens on your phone. There is no server, nothing to sign in to, and nothing to sync. Uninstall the app and every trace of it is gone.

## Download

Grab the latest APK from the [Releases page](https://github.com/mdshakib007/AppWall/releases/latest). Requires Android 10 or newer. The APK is about 3.5 MB.

## Installing

AppWall is not on Google Play yet. Sideloaded apps that use an accessibility service are blocked by Google Play Protect's fraud protection in a growing number of countries, so you may see "App blocked to protect your device". Two ways past it:

- **Install with adb** (Play Protect doesn't intercept it): enable USB debugging, then `adb install AppWall-v1.0.0.apk`.
- **Pause Play Protect scanning**: Play Store → your avatar → *Play Protect* → gear icon → turn off *Scan apps with Play Protect* → install → turn it back on. The check only runs at install time.

After installing, if the accessibility toggle is greyed out with a "Restricted setting" message: Settings → Apps → AppWall → ⋮ → *Allow restricted settings*, then enable the service.

## Features

- **Block apps.** Pick from installed apps; AppWall suggests the usual time-eaters it finds on your phone.
- **Block websites.** Type a domain, or tap popular sites grouped by category. Blocking `facebook.com` also blocks `m.facebook.com`, `bd.facebook.com` and every other subdomain.
- **Every browser, every in-app browser.** Blocking happens at the name-lookup level on the phone, so a blocked site simply fails to load in Chrome, Firefox, Brave, Edge, Opera, Samsung Internet and the rest, and inside the built-in browsers of apps like Messenger, Facebook, Instagram and Telegram. Your browser is never hijacked: you can still type, edit and navigate freely.
- **Schedules.** Forever, 1 hour to 1 year, a specific date, or daily time windows on chosen weekdays (windows can cross midnight). Edit any time.
- **Focus Mode.** A one-way commitment: choose a number of days and every block is locked for that period. You can add more, but nothing can be edited or removed, and AppWall keeps you out of its own system-settings pages so there is no quick escape hatch.
- **Insights.** Screen time today and this week, top apps, top websites, attempts blocked, and an honest estimate of the time you got back, with the formula explained in the app.
- **Confirm before you commit.** Suggested apps and sites are staged and applied only when you tap *Block*, so a stray tap during Focus Mode can't lock in something by accident.
- **Clean and quick.** Light and dark themes, one orange accent, smooth transitions, no bloat.

## How blocking works

| Layer | Mechanism | Covers |
|---|---|---|
| Apps | An accessibility service notices which app comes to the front and shows the *Blocked* screen over it. | Every app |
| Websites | A local, DNS-only `VpnService`. Only name lookups pass through it. Blocked names get *NXDOMAIN*, so the page fails to load; everything else is handed to Android's own resolver unchanged. | Every browser, every in-app browser, every app |

The DNS filter uses Android's VPN slot, so it can't run alongside another VPN. It never sees, routes or logs any traffic other than DNS lookups, and it never contacts any host other than the resolver your network already uses. The accessibility service only *reads* the browser address bar, for statistics; it never interrupts browsing.

## Privacy, and how to verify it

- **No server, no account, no analytics, no crash reporting, no third-party SDKs.**
- **Backups are disabled.** Your blocklist and statistics live in the app's private storage and are deleted on uninstall.
- **One network permission, one reason.** Android will not forward DNS lookups on an app's behalf without the `INTERNET` permission. That is the only thing AppWall uses it for. The only code that touches the network is [`DnsFilterVpnService.kt`](app/src/main/java/io/github/mdshakib007/appwall/service/DnsFilterVpnService.kt). Turn the website filter off in Settings and the app makes no network requests at all.
- **The accessibility service** looks at two things: which package is in front, and the address bar of browsers (for statistics only). It never interrupts your browser and never records keystrokes, messages or page content. It is [one short file](app/src/main/java/io/github/mdshakib007/appwall/service/AppWallAccessibilityService.kt).
- **Usage access** is optional and only powers the Insights tab.

### Permissions

| Permission | Why |
|---|---|
| Accessibility service | See the foreground app and the browser address bar. Required for blocking. |
| Display over other apps | Show the *Blocked* screen on top of a blocked app. Required. |
| VPN (website filter) | On-device DNS filter that makes blocked sites fail to load everywhere. Required for website blocking. |
| Usage access | Screen-time insights. Optional. |
| Notifications | Android requires a silent status notification while the DNS filter runs. |

Known limit: if Private DNS is set to a specific provider in Android's network settings, Android sends lookups straight to that provider and the DNS layer can't see them. AppWall detects this and shows a warning.

## Building from source

Requirements: JDK 17 and the Android SDK (platform 36). Then:

```bash
./gradlew :app:assembleDebug        # debug APK  -> app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # unit tests: domain matching, schedules, DNS codec
./gradlew :app:assembleRelease      # minified release APK (debug-signed unless a keystore is configured)
```

## Releasing

Nothing runs automatically; both workflows are started by hand from the **Actions** tab.

- **Build** → *Run workflow*: runs the unit tests and builds a debug APK you can download from the run's artifacts.
- **Release** → *Run workflow* → enter a version such as `1.2.3`: runs the tests, builds a signed release APK, creates the tag `v1.2.3` on the selected branch, and publishes a GitHub Release with the APK, its SHA-256, and auto-generated notes.

The version name is what you typed and the version code is derived from it (`1.2.3` → `10203`), so keep versions ascending.

### One-time signing setup

1. Create a keystore (keep it safe; losing it means users can't update in place):

   ```bash
   keytool -genkeypair -v -keystore release.jks -alias appwall -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Add four repository secrets under *Settings → Secrets and variables → Actions*:

   | Secret | Value |
   |---|---|
   | `APPWALL_KEYSTORE_BASE64` | output of `base64 -i release.jks` |
   | `APPWALL_KEYSTORE_PASSWORD` | keystore password |
   | `APPWALL_KEY_ALIAS` | `appwall` (or whatever you chose) |
   | `APPWALL_KEY_PASSWORD` | key password |

Until the secrets exist, release builds are signed with a throwaway debug key, which is fine for testing but not for distributing.

## Project layout

```
app/src/main/java/io/github/mdshakib007/appwall/
├── core/      pure logic: domain matching, schedule rules, DNS codec, block state, savings model
├── data/      Room database, DataStore prefs, usage stats, installed apps, curated catalogs
├── service/   accessibility service, DNS-filter VpnService, boot receiver
└── ui/        Jetpack Compose screens and the shared design components
```

Stack: Kotlin, Jetpack Compose, Material 3, Room, DataStore, Coroutines. No other dependencies.

## Contributing

Issues and pull requests are welcome. Easy wins:

- Add the address-bar view id of a browser that is missing from `Catalog.browserUrlBarIds`.
- Add popular sites or apps to `Catalog.kt`.
- Translations.

Please keep the two promises that define this project: **no network calls except DNS forwarding, and no third-party SDKs.**

## Privacy policy

[PRIVACY.md](PRIVACY.md) — no data collected, ever. Also used as the Google Play privacy policy URL.

## License

[MIT](LICENSE)
