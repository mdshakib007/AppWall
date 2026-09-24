<p align="center">
  <img src="app/src/main/res/drawable-nodpi/logo.webp" width="120" alt="AppWall logo">
</p>

<h1 align="center">AppWall</h1>

<p align="center"><b>App &amp; website blocker for Android. Free, open source, no server, no account, no tracking.</b></p>

AppWall keeps you away from the apps and websites that eat your time. Block anything permanently, for a few hours, until a date, or on a daily schedule. When you are serious, turn on **Focus Mode** and lock the whole list for days or weeks with no way out.

Everything happens on your phone. There is no server to talk to, nothing to sign in to, and nothing to sync. Uninstall the app and every trace of it is gone.

## Features

- **Block apps** – pick from installed apps; AppWall suggests the usual time-eaters it finds on your phone.
- **Block websites** – type a domain, or one-tap popular sites grouped by category. Blocking `facebook.com` also blocks `m.facebook.com`, `bd.facebook.com` and every other subdomain.
- **Works in every browser** – Chrome, Firefox, Brave, Edge, Opera, Samsung Internet, DuckDuckGo, Vivaldi, Kiwi and more, plus in-app browsers, thanks to two independent layers (see below).
- **Schedules** – forever, 1 hour to 1 year, a specific date, or daily windows on chosen weekdays (including windows that cross midnight). Editable any time.
- **Focus Mode** – a one-way commitment: choose a number of days, and every block becomes locked for that period. You can add more, but nothing can be edited or removed, and AppWall keeps you out of its own system-settings pages so there is no quick escape hatch.
- **Insights** – screen time today and for the week, top apps, top websites, attempts blocked, and an honest estimate of the time you got back (with the formula explained in-app).
- **Light and dark theme**, orange accent, Material 3, small APK.

## How blocking works

| Layer | Mechanism | What it covers |
|---|---|---|
| Apps | An accessibility service notices which app comes to the front and shows the *Blocked* screen on top of it. | Every app. |
| Websites, layer 1 | The same service reads the address bar of known browsers and reacts immediately. | Browsers. |
| Websites, layer 2 | A local, DNS-only `VpnService`. Only DNS lookups pass through it. Blocked names get `NXDOMAIN`; everything else is handed to Android's own resolver unchanged. | Every app on the phone, including in-app browsers. |

The VPN never sees, routes or logs any traffic other than DNS lookups. No other host is ever contacted.

## Privacy, verifiably

- **No server, no account, no analytics, no crash reporting.**
- **Backups are disabled.** Your blocklist and statistics stay in the app's private storage and are deleted on uninstall.
- **One network permission, one reason.** Android requires `INTERNET` before it will forward DNS lookups on an app's behalf. That is the only thing AppWall uses it for, and the only code that touches the network is [`DnsFilterVpnService.kt`](app/src/main/java/io/github/mdshakib007/appwall/service/DnsFilterVpnService.kt). Turn the website filter off in Settings and the app makes no network requests at all.
- **The accessibility service** only looks at which package is in front and the text of a browser's address bar. It never records keystrokes, messages or page content. It is [one short file](app/src/main/java/io/github/mdshakib007/appwall/service/AppWallAccessibilityService.kt).
- **Usage access** is optional and only powers the Insights tab.

### Permissions

| Permission | Why |
|---|---|
| Accessibility service | See the foreground app and the browser address bar (required for blocking). |
| Display over other apps | Show the *Blocked* screen on top of a blocked app (required). |
| VPN (website filter) | On-device DNS filter for every app, including in-app browsers (recommended). |
| Usage access | Screen-time insights (optional). |
| Notifications | Android requires a quiet status notification while the DNS filter runs. |

## Building

Requirements: JDK 17, Android SDK 36. Then:

```bash
./gradlew :app:assembleDebug     # debug APK
./gradlew :app:assembleRelease   # minified release APK (signed with the debug key unless you configure a keystore)
./gradlew :app:testDebugUnitTest # unit tests for domain matching, schedules and the DNS codec
```

To sign real releases, provide `APPWALL_KEYSTORE`, `APPWALL_KEYSTORE_PASSWORD`, `APPWALL_KEY_ALIAS` and `APPWALL_KEY_PASSWORD` as Gradle properties or environment variables.

Minimum Android version: 10 (API 29).

## Project layout

```
app/src/main/java/io/github/mdshakib007/appwall/
├── core/        pure logic: domain matching, schedule rules, DNS codec, block state, savings model
├── data/        Room database, DataStore prefs, usage stats, installed apps, curated catalogs
├── service/     accessibility service, DNS-filter VpnService, boot receiver
└── ui/          Jetpack Compose screens (onboarding, home, add, detail, focus, insights, settings, blocked)
```

## Contributing

Issues and pull requests are welcome. Good first contributions:

- Add the address-bar view id of a browser that is not yet in `Catalog.browserUrlBarIds`.
- Add popular sites or apps to `Catalog.kt`.
- Translations (strings are in `res/values/strings.xml` and, for now, in the Compose screens).

Please keep the two promises that define this project: **no network calls except DNS forwarding, and no third-party SDKs.**

## License

[MIT](LICENSE)
