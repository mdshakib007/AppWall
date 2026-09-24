# AppWall Privacy Policy

**Effective date: 24 September 2026**

AppWall is a free, open-source app and website blocker for Android, published by Md Shakib. This policy explains what the app does with information on your device. The short version: **AppWall does not collect, store off-device, share, or sell any personal data. There is no server, no account, no analytics, and no advertising.**

## Data the app stores on your device

AppWall keeps the following in its private app storage on your phone only:

- The apps and websites you choose to block, and their schedules.
- Focus Mode start and end times.
- A log of blocked attempts (which blocked item, and when) and time spent on websites, used for the Insights screen.
- Your settings (theme, whether the website filter is on).

This data never leaves your device. Cloud backup and device-to-device transfer are disabled for the app. Uninstalling AppWall deletes all of it.

## Permissions and what they are used for

| Permission | Use |
|---|---|
| Accessibility service | To know which app is in the foreground so a blocked app can be closed and the Blocked screen shown, and to read the address bar of web browsers so the app can count blocked attempts and measure time per website for your own statistics. It does not read, record, or transmit keystrokes, messages, passwords, or page content. |
| Display over other apps | To show the Blocked screen on top of a blocked app. |
| VPN (VpnService) | To run a local, on-device DNS filter that makes blocked websites fail to load. Only DNS name lookups pass through it. Blocked names are answered on the device; all other lookups are forwarded unchanged to the DNS resolver your network already uses. No traffic is routed, inspected, logged, or sent to any other server. |
| Usage access | To show screen-time statistics on the Insights screen. Read on demand, never stored elsewhere. |
| Query all packages | To list your installed apps so you can choose which to block. |
| Internet | Required by Android for the DNS filter to forward lookups to your network's resolver. AppWall contacts no other host. |
| Notifications | For the silent status notification Android requires while the DNS filter runs. |

## Data sharing

AppWall does not share any data with anyone. It has no third-party SDKs, no analytics, no crash reporting, and no advertising.

## Children

AppWall does not knowingly collect information from anyone, including children.

## Open source

The complete source code is available at https://github.com/mdshakib007/AppWall so that anyone can verify these statements.

## Changes

If this policy changes, the new version will be published at the same address in the repository with an updated effective date.

## Contact

Questions: open an issue at https://github.com/mdshakib007/AppWall/issues.
