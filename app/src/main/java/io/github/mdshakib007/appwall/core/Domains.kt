package io.github.mdshakib007.appwall.core

import java.util.Locale

/**
 * Pure functions for website handling. No Android dependencies so they are unit-testable.
 */
object Domains {

    private val hostRegex = Regex("^(?=.{1,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}$")

    /**
     * Turns whatever the user typed ("https://www.M.Facebook.com/groups/x?y=1", "facebook.com/") into a
     * canonical registrable-ish host: lowercase, no scheme, no path, no port, no leading "www.".
     * Returns null when the input is not a plausible domain.
     */
    fun normalize(raw: String): String? {
        var s = raw.trim().lowercase(Locale.ROOT)
        if (s.isEmpty()) return null
        s = s.substringAfter("://", s)
        s = s.substringBefore('/').substringBefore('?').substringBefore('#')
        s = s.substringAfterLast('@') // user:pass@host
        s = s.substringBefore(':')     // port
        s = s.trimEnd('.')
        if (s.startsWith("www.")) s = s.removePrefix("www.")
        if (s.startsWith("m.") && s.count { it == '.' } >= 2) s = s.removePrefix("m.")
        if (!hostRegex.matches(s)) return null
        return s
    }

    /** True when [host] equals [blocked] or is any subdomain of it. */
    fun matches(host: String, blocked: String): Boolean {
        val h = host.lowercase(Locale.ROOT).trimEnd('.')
        return h == blocked || h.endsWith(".$blocked")
    }

    /** Returns the first blocked domain that [host] falls under, or null. */
    fun findBlocked(host: String, blockedDomains: Collection<String>): String? {
        val h = host.lowercase(Locale.ROOT).trimEnd('.')
        if (h.isEmpty()) return null
        for (b in blockedDomains) if (h == b || h.endsWith(".$b")) return b
        return null
    }

    /**
     * Extracts a host from a browser address bar string. Address bars show "facebook.com/home",
     * "https://m.facebook.com", "Search or type URL" etc. Returns null when the text is not URL-like.
     */
    fun hostFromAddressBar(text: String): String? {
        val t = text.trim()
        if (t.isEmpty() || t.contains(' ') || t.contains('\n')) return null
        val afterScheme = t.substringAfter("://", t)
        val host = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfterLast('@').substringBefore(':').lowercase(Locale.ROOT).trimEnd('.')
        if (host.isEmpty() || !host.contains('.')) return null
        if (!hostRegex.matches(host)) return null
        return host
    }

    /**
     * Like [hostFromAddressBar] but for the title strip of an in-app browser, which often decorates the host
     * with a lock glyph or similar ("🔒 m.facebook.com", "· facebook.com"). Leading/trailing non-URL characters
     * are dropped; anything that still contains whitespace (a title, a chat message) is rejected.
     */
    fun hostFromToolbarText(text: String): String? {
        val t = text.trim().trimStart { !(it.isLetterOrDigit()) }.trimEnd { !(it.isLetterOrDigit() || it == '/') }
        return hostFromAddressBar(t)
    }

    /**
     * For address bars that only expose their URL through an accessibility description that also carries a label,
     * e.g. Firefox's Compose toolbar: " duckduckgo.com. Search or enter address". Returns the first whitespace-
     * separated token that is a host / URL. Only used on nodes already identified as the address bar.
     */
    fun hostFromAddressBarDescription(description: String): String? {
        for (raw in description.split(' ', '\n', '\t')) {
            val token = raw.trim().trimEnd('.', ',', ';', ':')
            if (token.length < 4 || !token.contains('.')) continue
            hostFromAddressBar(token)?.let { return it }
        }
        return null
    }

    /** "facebook.com" -> "Facebook", "m.youtube.com" -> "Youtube". Purely cosmetic. */
    fun prettyName(domain: String): String {
        val core = domain.split('.').let { parts ->
            if (parts.size >= 2) parts[parts.size - 2] else parts.first()
        }
        return core.replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}
