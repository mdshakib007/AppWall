# AppWall — Build Plan & Progress Tracker

> App & Website Blocker for Android. Free, open source, no server, no tracking, no analytics, no network calls.
> Repo: https://github.com/mdshakib007/AppWall

Legend: `[ ]` todo · `[~]` in progress · `[x]` done · `[!]` blocked / needs decision

---

## 0. Feasibility verdict

Everything requested is buildable on stock Android with **zero network calls from the app**. Two honest limits, neither a blocker:

| Concern | Reality | What we do |
|---|---|---|
| Web blocking in *every* browser + in-app browsers (Messenger etc.) | Done at DNS level with a **local, DNS-only VpnService** (no traffic leaves the device through us; we only answer/forward DNS to the network's own resolver). Subdomains match automatically. | Plus an Accessibility layer that reads browser URL bars for an instant, pretty "Blocked" screen. |
| User set **Private DNS = strict hostname** (Settings → Network) | Android sends DNS over TLS straight to that host, bypassing any local filter. Same for a custom DoH provider inside Chrome. | Detect it and show a clear warning card with a one-tap link to the setting. Accessibility URL layer still blocks known browsers. |
| Bypassing Focus Mode by disabling the service / uninstalling | No app can fully stop this without Device Admin. | Phase 6: during Focus Mode, guard Settings pages for AppWall (accessibility page, app-info, VPN) via accessibility. Optional "uninstall protection" (Device Admin) later. |
| App size | Kotlin + Compose + Room, no third-party SDKs, vector art only, R8 + resource shrinking. | Target **< 8 MB** release APK (checked in Phase 8). |

## 1. Architecture (decided)

- **Language/UI:** Kotlin, Jetpack Compose, Material 3, orange seed color, light + dark (follows system, overridable).
- **Package:** `io.github.mdshakib007.appwall` · minSdk 29 (Android 10; required by the DnsResolver API that lets the DNS filter work without INTERNET permission) · target/compileSdk 36.
- **Storage:** Room (blocklist, schedules, usage events, focus session). DataStore for settings. Everything on-device; uninstall = gone.
- **App blocking:** `AccessibilityService` watches foreground package → if blocked & active now → launch full-screen `BlockedActivity` (exempt from background-launch limits via "Display over other apps").
- **Website blocking (layer 1, universal):** `VpnService` that routes **only** a fake DNS IP (10.111.222.3/32) through the tunnel. UDP DNS queries are parsed; blocked domain (or any subdomain) → NXDOMAIN; everything else forwarded to the underlying network's real DNS via a protected socket. No other traffic touches the VPN.
- **Website blocking (layer 2, UX):** same Accessibility service reads URL bar of known browsers → instant Blocked screen + navigate away.
- **Schedules:** per-item: Always · Until date/time (hours/days/months/years) · Daily schedule (days + time ranges). Editable unless locked by Focus Mode.
- **Focus Mode:** global commitment for N days. Locks every current block item (no edit/remove), allows adding. Countdown on home + dashboard. Not cancellable.
- **Insights:** `UsageStatsManager` for app screen time; accessibility-derived time per website; blocked-attempt log; "time saved" = baseline daily use (7-day avg before block) × days blocked, falling back to attempts × your average session length. Explained in-app.
- **Onboarding:** 4 illustrated cards (what it does · nothing leaves your phone · open source + GitHub link · how blocking works) → permission steps (Accessibility, Usage Access, Display over apps, VPN consent, Notifications).
- **Permissions requested:** BIND_ACCESSIBILITY_SERVICE, PACKAGE_USAGE_STATS, SYSTEM_ALERT_WINDOW, BIND_VPN_SERVICE, FOREGROUND_SERVICE(+SYSTEM_EXEMPTED), POST_NOTIFICATIONS, QUERY_ALL_PACKAGES, RECEIVE_BOOT_COMPLETED, ACCESS_NETWORK_STATE, **INTERNET** (see decision log 2026-09-24: required by Android for the DNS forwarder; only `DnsFilterVpnService.kt` touches the network).

## 2. Screens

1. Onboarding (cards + permissions)
2. Home / Blocklist — active blocks (apps + sites), status chips, FAB "Add"
3. Add — tabs: Websites (input + popular sites) · Apps (suggested time-killers installed · all apps w/ search)
4. Block details — schedule editor, stats for that item, remove (unless locked)
5. Focus Mode — start (choose days) / active countdown / history
6. Insights — today/week screen time, top apps, sites, attempts blocked, time saved
7. Settings — theme, permissions health, Private DNS warning, about/GitHub/licenses
8. Blocked screen (full-screen interstitial)

## 3. Phases & progress

### Phase 1 — Project scaffold
- [x] Gradle project (AGP 8.13, Kotlin 2.2, Compose BOM, Room+KSP, DataStore, Navigation)
- [x] Theme: orange M3 palette light/dark, typography, shapes
- [x] Adaptive launcher icon from `logo.png` (transparent bg)
- [x] Builds & installs on emulator

### Phase 2 — Data layer
- [x] Room entities: BlockItem, Schedule, BlockAttempt, UsageSession, FocusSession
- [x] Repository + "is blocked right now?" rule engine (unit-tested)
- [x] Domain normalizer (strip scheme/www/path, lowercase) + subdomain matcher (unit-tested)
- [x] Installed-apps provider + curated "popular time killers" list + popular websites list

### Phase 3 — Blocking engine
- [x] AccessibilityService: foreground app detection → BlockedActivity
- [x] Browser URL-bar detection for Chrome, Firefox, Brave, Edge, Opera, Samsung, DuckDuckGo, Vivaldi, Kiwi
- [x] DNS-filter VpnService (UDP DNS parse/forward/NXDOMAIN), foreground notification, boot restart
- [x] BlockedActivity UI (why blocked, until when, "Go home")
- [x] Verified on emulator: blocked app (YouTube), blocked site in Chrome (m.facebook.com), subdomain via DNS (bd.facebook.com → NXDOMAIN). In-app WebView: covered by the DNS layer by design; not exercised on the emulator (no such app installed) — [ ] verify on a real device with Messenger.

### Phase 4 — UI
- [x] Onboarding cards + illustrations + permission flow
- [x] Home / Blocklist
- [x] Add flow (websites + apps)
- [x] Block details + schedule editor
- [x] Settings + About

### Phase 5 — Focus Mode
- [x] Start flow (days picker, confirmation), lock semantics in repo + UI
- [x] Countdown card, completion state

### Phase 6 — Hardening
- [x] Guard AppWall's own Settings pages during Focus Mode
- [x] Private DNS strict-mode detection + warning
- [x] Re-establish VPN on blocklist change (flush caches)

### Phase 7 — Insights
- [x] UsageStats collection, website session tracking
- [x] Dashboard charts (Compose canvas, no chart lib)
- [x] Time-saved model + explanation sheet

### Phase 8 — Release polish
- [x] R8/shrink, release APK size check (< 8 MB) — **3.4 MB** release APK
- [x] README with privacy explanation (INTERNET permission rationale), permissions table, contributing
- [x] LICENSE (GPL-3.0 or MIT — owner's call, default MIT)
- [x] Emulator walkthrough: onboarding, permissions, add sites/apps, block screen, detail, focus start, settings guard, insights, settings, dark mode, release build smoke test

## 4. Decisions log
- 2026-09-24 · **INTERNET permission is required.** Verified on the emulator: without it both a protected `DatagramSocket` and `DnsResolver.rawQuery` fail with EPERM, so a DNS filter cannot forward the lookups it doesn't block. Options were (a) keep universal website blocking incl. in-app browsers and declare INTERNET, or (b) drop the DNS layer and block only via browser address bars. Chose (a) because in-app-browser blocking was a day-1 requirement; trust is preserved by open source + a single network-touching file + optional filter. Reversal = remove the permission and the service (2 lines).
- 2026-09-24 · DNS forwarding uses `android.net.DnsResolver` (system resolver on the underlying network) rather than raw sockets: identical behaviour to a phone without AppWall (Private DNS, caching, IPv6), fewer lines. Sets minSdk 29.
- 2026-09-24 · Package id `io.github.mdshakib007.appwall` (provably owned via GitHub, F-Droid friendly).
- 2026-09-24 · DNS-only VPN over full-traffic VPN: tiny, no battery cost, no traffic inspection = matches "no spy" promise.
- 2026-09-24 · Block screen is an Activity (needs "Display over other apps"), not an accessibility overlay: proper theming, back-handling, animations.
- 2026-09-24 · No chart / image libraries; everything drawn with Compose to keep APK small.

## 5. Status (2026-09-24)
v1 feature-complete and verified on the API 37 emulator. Remaining before a public release:
- [ ] Test on a real device: Messenger in-app browser, Samsung/MIUI settings-guard package names, battery behaviour of the VPN service overnight.
- [ ] Sign with a real keystore (see `app/build.gradle.kts`), bump versionCode, tag `v1.0.0`, publish APK on GitHub Releases; consider F-Droid submission.
- [ ] Move remaining hardcoded UI strings into `strings.xml` for translations.
- [ ] Optional: Device Admin "uninstall protection" during Focus Mode.

## 6. Open questions for the owner (non-blocking, defaults applied)
- License: defaulting to **MIT**. Say if you prefer GPL-3.0.
- Uninstall protection via Device Admin: **not** in v1 (defaults to Settings-guard only).
