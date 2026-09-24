1. Create the developer account: personal account, pay the one-time US$25, complete identity verification (ID photo and possibly a bank or utility document). Approval can take a day or two, so start this first.
2. Create app: name "AppWall", free, app type "App".
3. Store listing: short description, full description, upload the icon and feature graphic, and at least 2 phone screenshots. Take those directly on your phone: home, add page, blocked screen, insights.
4. App content section, fill each item:
   - Privacy policy: the PRIVACY.md URL above.
   - Data safety: "No data collected, no data shared."
   - Ads: no. Target audience: 18+ (avoids the families policy). Content rating questionnaire: utility app, no sensitive content.
   - Accessibility service declaration: purpose "digital wellbeing / blocking distracting apps chosen by the user"; mention the in-app disclosure.
   - VpnService declaration: purpose "on-device content filtering; no traffic tunneled to a remote server."
   - QUERY_ALL_PACKAGES declaration: "app blocker; core function is choosing which installed apps to block."
   - Permissions declaration for Usage access: "screen-time insights."
5. Testing → Closed testing: create a track, upload the .aab, add testers by email. New personal accounts need 12 opted-in testers for 14 continuous days before production access is unlocked, so line up friends now. This waiting period is the real bottleneck, not the paperwork.
6. After the 14 days: apply for production access, then promote the build to Production and submit for review. Review usually takes a few days; the accessibility and VPN declarations may get a follow-up question.
