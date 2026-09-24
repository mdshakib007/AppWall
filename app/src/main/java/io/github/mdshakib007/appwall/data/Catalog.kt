package io.github.mdshakib007.appwall.data

/** Curated, static suggestions. Purely local data; edit freely via pull requests. */
object Catalog {

    class SiteSuggestion(val name: String, val domains: List<String>, val emoji: String) {
        val primary get() = domains.first()
    }

    class SiteGroup(val title: String, val sites: List<SiteSuggestion>)

    private fun s(name: String, emoji: String, vararg domains: String) = SiteSuggestion(name, domains.toList(), emoji)

    val siteGroups: List<SiteGroup> = listOf(
        SiteGroup(
            "Social & video", listOf(
                s("Facebook", "👥", "facebook.com", "fb.com", "fb.watch"),
                s("Instagram", "📸", "instagram.com"),
                s("YouTube", "▶️", "youtube.com", "youtu.be"),
                s("TikTok", "🎵", "tiktok.com"),
                s("X / Twitter", "🐦", "x.com", "twitter.com"),
                s("Reddit", "👽", "reddit.com", "redd.it"),
                s("Snapchat", "👻", "snapchat.com"),
                s("Threads", "🧵", "threads.com", "threads.net"),
                s("Pinterest", "📌", "pinterest.com"),
                s("Twitch", "🎮", "twitch.tv"),
                s("Discord", "💬", "discord.com", "discord.gg"),
                s("Tumblr", "📝", "tumblr.com"),
                s("9GAG", "😂", "9gag.com"),
                s("Bilibili", "📺", "bilibili.com"),
                s("Quora", "❓", "quora.com"),
                s("LinkedIn", "💼", "linkedin.com"),
                s("Imgur", "🖼️", "imgur.com"),
                s("Kwai", "🎬", "kwai.com"),
            )
        ),
        SiteGroup(
            "Streaming", listOf(
                s("Netflix", "🍿", "netflix.com"),
                s("Prime Video", "📦", "primevideo.com"),
                s("Disney+", "🏰", "disneyplus.com"),
                s("Hulu", "📼", "hulu.com"),
                s("Crunchyroll", "🍥", "crunchyroll.com"),
                s("Dailymotion", "🎞️", "dailymotion.com"),
            )
        ),
        SiteGroup(
            "Shopping", listOf(
                s("Amazon", "🛒", "amazon.com"),
                s("AliExpress", "🛍️", "aliexpress.com"),
                s("eBay", "🏷️", "ebay.com"),
                s("Temu", "🧾", "temu.com"),
                s("Daraz", "🛒", "daraz.com", "daraz.com.bd", "daraz.pk", "daraz.lk", "daraz.com.np"),
                s("Shein", "👗", "shein.com"),
            )
        ),
        SiteGroup(
            "Games", listOf(
                s("Roblox", "🟥", "roblox.com"),
                s("Steam", "♨️", "steampowered.com", "steamcommunity.com"),
                s("Poki", "🕹️", "poki.com"),
                s("Miniclip", "🎯", "miniclip.com"),
                s("Chess.com", "♟️", "chess.com"),
            )
        ),
        SiteGroup(
            "Adult", listOf(
                s("Pornhub", "🔞", "pornhub.com"),
                s("XVideos", "🔞", "xvideos.com"),
                s("XNXX", "🔞", "xnxx.com"),
                s("xHamster", "🔞", "xhamster.com"),
                s("OnlyFans", "🔞", "onlyfans.com"),
                s("RedTube", "🔞", "redtube.com"),
                s("YouPorn", "🔞", "youporn.com"),
                s("Chaturbate", "🔞", "chaturbate.com"),
                s("Stripchat", "🔞", "stripchat.com"),
                s("Brazzers", "🔞", "brazzers.com"),
            )
        ),
        SiteGroup(
            "Gambling", listOf(
                s("bet365", "🎰", "bet365.com"),
                s("1xBet", "🎰", "1xbet.com"),
                s("Stake", "🎰", "stake.com"),
                s("Betway", "🎰", "betway.com"),
                s("Melbet", "🎰", "melbet.com"),
                s("Parimatch", "🎰", "parimatch.com"),
            )
        ),
        SiteGroup(
            "News", listOf(
                s("CNN", "📰", "cnn.com"),
                s("BBC", "📰", "bbc.com", "bbc.co.uk"),
                s("Daily Mail", "📰", "dailymail.co.uk"),
                s("Fox News", "📰", "foxnews.com"),
                s("NY Times", "📰", "nytimes.com"),
                s("Prothom Alo", "📰", "prothomalo.com"),
            )
        ),
    )

    /** Package names of famously time-consuming apps; shown as suggestions when installed. Order = priority. */
    val timeKillerPackages: List<String> = listOf(
        "com.facebook.katana", "com.facebook.lite", "com.instagram.android", "com.instagram.lite",
        "com.google.android.youtube", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go",
        "com.twitter.android", "com.reddit.frontpage", "com.snapchat.android", "com.instagram.barcelona",
        "com.pinterest", "tv.twitch.android.app", "com.discord", "com.tumblr", "com.ninegag.android.app",
        "com.facebook.orca", "com.facebook.mlite", "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger",
        "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient", "com.disney.disneyplus", "com.hulu.plus",
        "com.crunchyroll.crunchyroid", "tv.danmaku.bili", "com.kwai.video", "com.kwai.bulldog",
        "com.linkedin.android", "com.quora.android", "com.imgur.mobile",
        "com.roblox.client", "com.supercell.clashofclans", "com.supercell.clashroyale", "com.supercell.brawlstars",
        "com.dts.freefireth", "com.dts.freefiremax", "com.tencent.ig", "com.pubg.imobile", "com.pubg.krmobile",
        "com.mobile.legends", "com.king.candycrushsaga", "com.miHoYo.GenshinImpact", "com.activision.callofduty.shooter",
        "com.garena.game.codm", "com.ea.gp.fifamobile", "com.innersloth.spacemafia", "com.mojang.minecraftpe",
        "com.amazon.mShop.android.shopping", "com.alibaba.aliexpresshd", "com.daraz.android", "com.einnovation.temu",
        "com.zzkko", "com.ebay.mobile",
        "com.tinder", "com.bumble.app", "com.badoo.mobile",
        "com.google.android.apps.youtube.music", "com.spotify.music",
    )

    /**
     * Browsers we can read the address bar of. Value = resource id of the URL field.
     * Unknown browsers fall back to a heuristic (any node whose text looks like a URL).
     */
    val browserUrlBarIds: Map<String, List<String>> = mapOf(
        "com.android.chrome" to listOf("com.android.chrome:id/url_bar"),
        "com.chrome.beta" to listOf("com.chrome.beta:id/url_bar"),
        "com.chrome.dev" to listOf("com.chrome.dev:id/url_bar"),
        "com.chrome.canary" to listOf("com.chrome.canary:id/url_bar"),
        "org.chromium.chrome" to listOf("org.chromium.chrome:id/url_bar"),
        "org.mozilla.firefox" to listOf("org.mozilla.firefox:id/mozac_browser_toolbar_url_view", "org.mozilla.firefox:id/url_bar_title"),
        "org.mozilla.firefox_beta" to listOf("org.mozilla.firefox_beta:id/mozac_browser_toolbar_url_view"),
        "org.mozilla.fenix" to listOf("org.mozilla.fenix:id/mozac_browser_toolbar_url_view"),
        "org.mozilla.focus" to listOf("org.mozilla.focus:id/mozac_browser_toolbar_url_view", "org.mozilla.focus:id/display_url"),
        "com.brave.browser" to listOf("com.brave.browser:id/url_bar"),
        "com.microsoft.emmx" to listOf("com.microsoft.emmx:id/url_bar"),
        "com.opera.browser" to listOf("com.opera.browser:id/url_field"),
        "com.opera.mini.native" to listOf("com.opera.mini.native:id/url_field"),
        "com.opera.gx" to listOf("com.opera.gx:id/addressbarEdit"),
        "com.sec.android.app.sbrowser" to listOf("com.sec.android.app.sbrowser:id/location_bar_edit_text"),
        "com.duckduckgo.mobile.android" to listOf("com.duckduckgo.mobile.android:id/omnibarTextInput"),
        "com.vivaldi.browser" to listOf("com.vivaldi.browser:id/url_bar"),
        "com.kiwibrowser.browser" to listOf("com.kiwibrowser.browser:id/url_bar"),
        "com.mi.globalbrowser" to listOf("com.mi.globalbrowser:id/url"),
        "com.android.browser" to listOf("com.android.browser:id/url"),
        "com.huawei.browser" to listOf("com.huawei.browser:id/url_bar"),
        "com.UCMobile.intl" to listOf("com.UCMobile.intl:id/url_bar"),
        "com.uc.browser.en" to listOf("com.uc.browser.en:id/url_bar"),
        "com.yandex.browser" to listOf("com.yandex.browser:id/bro_omnibar_address_title_text"),
        "com.ecosia.android" to listOf("com.ecosia.android:id/url_bar"),
        "com.cloudmosa.puffinFree" to listOf("com.cloudmosa.puffinFree:id/url_bar"),
        "com.nhn.android.search" to listOf("com.nhn.android.search:id/url"),
        "com.sec.android.app.sbrowser.beta" to listOf("com.sec.android.app.sbrowser.beta:id/location_bar_edit_text"),
        "org.torproject.torbrowser" to listOf("org.torproject.torbrowser:id/mozac_browser_toolbar_url_view"),
        "com.qwant.liberty" to listOf("com.qwant.liberty:id/mozac_browser_toolbar_url_view"),
        "com.kiwibrowser.browser.dev" to listOf("com.kiwibrowser.browser.dev:id/url_bar"),
    )

    /** Packages that must never be blocked (would brick the experience). */
    val neverBlock: Set<String> = setOf(
        "android", "com.android.systemui", "com.android.settings", "com.android.phone", "com.android.dialer",
        "com.google.android.dialer", "com.android.launcher", "com.android.launcher3", "com.google.android.apps.nexuslauncher",
        "com.android.packageinstaller", "com.google.android.packageinstaller", "com.android.permissioncontroller",
        "com.google.android.permissioncontroller", "com.android.emergency", "com.android.inputmethod.latin",
        "com.google.android.inputmethod.latin", "com.android.vpndialogs", "com.google.android.gms",
        "io.github.mdshakib007.appwall", "io.github.mdshakib007.appwall.debug",
    )
}
