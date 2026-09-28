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
- **Every browser, no VPN.** AppWall reads the browser's address bar. The moment a blocked address is committed, whether typed and entered, tapped in a link, or restored with the session, the navigation is cancelled before the page shows. Nothing is drawn over the browser and nothing happens while you are still typing, so the address bar stays entirely yours. Works in Chrome, Firefox, Brave, Edge, Opera, Samsung Internet, DuckDuckGo, Vivaldi and more, in Chrome Custom Tabs, and best-effort inside the in-app browsers of apps like Messenger and Facebook.
- **Schedules.** Forever, 1 hour to 1 year, a specific date, or daily time windows on chosen weekdays (windows can cross midnight). Edit any time.
- **Focus Mode.** A one-way commitment: choose a number of days and your list is locked for that period. Every block keeps its own schedule, you can add more or extend one that ended, but nothing can be relaxed or removed, and AppWall keeps you out of its own system-settings pages so there is no quick escape hatch.
- **Insights.** Screen time today and this week, top apps, top websites, attempts blocked, and an honest estimate of the time you got back, with the formula explained in the app.
- **Confirm before you commit.** Suggested apps and sites are staged and applied only when you tap *Block*, so a stray tap during Focus Mode can't lock in something by accident.
- **Clean and quick.** Light and dark themes, one orange accent, smooth transitions, no bloat.

## How blocking works

| Layer | Mechanism | Covers |
|---|---|---|
| Apps | The accessibility service notices which app comes to the front, sends it home and shows the *Blocked* screen. | Every app |
| Websites | The same service reads the address bar of the browser in front. When it shows a blocked address (and is not being edited), AppWall presses *Back*, which cancels a navigation that is still loading or returns an open page to the previous one. It checks again a moment later and repeats if needed. | Every browser with a readable address bar, Chrome Custom Tabs, in-app browsers that show the site's address |

Two details worth knowing:

- If *Back* leaves a browser minimised with the blocked tab still current (typically a restored session), the next launch would show that tab and bounce again. AppWall notices this and reopens the browser on a blank tab so it stays usable. Browsers don't let other apps close their tabs, so the blocked tab stays in the tab list, just never in front.
- This is the same approach BlockSite and similar apps use, and it shares their one limit: a page reached through a link can appear for a fraction of a second before it is cancelled, because browsers only reveal a link's destination once the navigation has started. Typed addresses are cancelled before anything loads.

## Privacy, and how to verify it

- **No server, no account, no analytics, no crash reporting, no third-party SDKs.**
- **No network permission.** AppWall does not declare `android.permission.INTERNET`, so the operating system itself makes it impossible for the app to send anything anywhere. Check *Settings › Apps › AppWall › Permissions* on your phone.
- **Backups are disabled.** Your blocklist and statistics live in the app's private storage and are deleted on uninstall.
- **The accessibility service** looks at two things: which package is in front, and the address a browser is showing. It never reads page content, messages or what you type. It is [one file](app/src/main/java/io/github/mdshakib007/appwall/service/AppWallAccessibilityService.kt).
- **Usage access** is optional and only powers the Insights tab.

### Permissions

| Permission | Why |
|---|---|
| Accessibility service | See the foreground app and the browser's address bar. Required for blocking apps and websites. |
| Display over other apps | Show the *Blocked* screen on top of a blocked app. Required. |
| Usage access | Screen-time insights. Optional. |

Known limits: a browser whose address bar AppWall cannot read (an unusual or brand-new one) falls back to a heuristic and may not be covered; in-app browsers are covered only when they display the site's address in their toolbar; and, as with every non-VPN blocker, a page opened from a link can flash briefly before it is cancelled.

## Building from source

Requirements: JDK 17 and the Android SDK (platform 36). Then:

```bash
./gradlew :app:assembleDebug        # debug APK  -> app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # unit tests: domain matching, address-bar parsing, schedules
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
├── core/      pure logic: domain matching, address-bar parsing, schedule rules, block state, savings model
├── data/      Room database, DataStore prefs, usage stats, installed apps, curated catalogs
├── service/   the accessibility service (all enforcement lives here)
└── ui/        Jetpack Compose screens and the shared design components
```

Stack: Kotlin, Jetpack Compose, Material 3, Room, DataStore, Coroutines. No other dependencies.

## Contributing

Issues and pull requests are welcome. Easy wins:

- Add the address-bar view id (or Compose test tag) of a browser that is missing from `Catalog.browserUrlBarIds`.
- Add popular sites or apps to `Catalog.kt`.
- Translations.

Please keep the two promises that define this project: **no network permission, and no third-party SDKs.**

## Privacy policy

[PRIVACY.md](PRIVACY.md) — no data collected, ever. Also used as the Google Play privacy policy URL.

## License

[MIT](LICENSE)
