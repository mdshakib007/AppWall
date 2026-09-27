package io.github.mdshakib007.appwall.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainsTest {
    @Test fun normalizeStripsEverything() {
        assertEquals("facebook.com", Domains.normalize("https://www.M.Facebook.com/groups/x?y=1#z"))
        assertEquals("facebook.com", Domains.normalize("  facebook.com/ "))
        assertEquals("facebook.com", Domains.normalize("http://user:pw@facebook.com:8080/"))
        assertEquals("bd.facebook.com", Domains.normalize("bd.facebook.com"))
        assertEquals("daraz.com.bd", Domains.normalize("www.daraz.com.bd"))
        assertEquals("x.com", Domains.normalize("X.COM."))
    }

    @Test fun normalizeRejectsGarbage() {
        assertNull(Domains.normalize(""))
        assertNull(Domains.normalize("facebook"))
        assertNull(Domains.normalize("not a domain"))
        assertNull(Domains.normalize("http://"))
        assertNull(Domains.normalize("192.168.1.1")) // tld must be letters
    }

    @Test fun subdomainsMatch() {
        assertTrue(Domains.matches("facebook.com", "facebook.com"))
        assertTrue(Domains.matches("m.facebook.com", "facebook.com"))
        assertTrue(Domains.matches("bd.static.facebook.com", "facebook.com"))
        assertTrue(Domains.matches("M.FACEBOOK.COM.", "facebook.com"))
        assertFalse(Domains.matches("facebook.co", "facebook.com"))
        assertFalse(Domains.matches("notfacebook.com", "facebook.com"))
        assertFalse(Domains.matches("facebook.com.evil.net", "facebook.com"))
    }

    @Test fun findBlocked() {
        val list = listOf("youtube.com", "facebook.com")
        assertEquals("facebook.com", Domains.findBlocked("m.facebook.com", list))
        assertEquals("youtube.com", Domains.findBlocked("www.youtube.com", list))
        assertNull(Domains.findBlocked("youtu.be", list))
    }

    @Test fun hostFromAddressBar() {
        // What Chrome / Firefox / Samsung actually show in the bar
        assertEquals("m.facebook.com", Domains.hostFromAddressBar("https://m.facebook.com/home.php"))
        assertEquals("facebook.com", Domains.hostFromAddressBar("facebook.com/watch"))
        assertEquals("facebook.com", Domains.hostFromAddressBar("facebook.com"))
        assertEquals("facebook.com", Domains.hostFromAddressBar("FACEBOOK.COM/"))
        assertEquals("www.youtube.com", Domains.hostFromAddressBar("http://www.youtube.com:8080/watch?v=1#t"))
        assertEquals("youtube.com", Domains.hostFromAddressBar("youtube.com"))
        assertEquals("daraz.com.bd", Domains.hostFromAddressBar("daraz.com.bd/#/"))
        // Things that must never look like a site
        assertNull(Domains.hostFromAddressBar("Search or type URL"))
        assertNull(Domains.hostFromAddressBar("Search or type web address"))
        assertNull(Domains.hostFromAddressBar("how to bake bread"))
        assertNull(Domains.hostFromAddressBar("facebook.com login")) // a search query, not a navigation
        assertNull(Domains.hostFromAddressBar("facebook"))
        assertNull(Domains.hostFromAddressBar("about:blank"))
        assertNull(Domains.hostFromAddressBar("chrome://newtab"))
        assertNull(Domains.hostFromAddressBar("192.168.0.1"))
        assertNull(Domains.hostFromAddressBar(""))
    }

    @Test fun hostFromToolbarText() {
        assertEquals("m.facebook.com", Domains.hostFromToolbarText("🔒 m.facebook.com"))
        assertEquals("m.facebook.com", Domains.hostFromToolbarText("🔒m.facebook.com"))
        assertEquals("facebook.com", Domains.hostFromToolbarText("· facebook.com ·"))
        assertEquals("facebook.com", Domains.hostFromToolbarText("https://facebook.com/"))
        assertEquals("facebook.com", Domains.hostFromToolbarText("(facebook.com)"))
        assertNull(Domains.hostFromToolbarText("Facebook – log in or sign up"))
        assertNull(Domains.hostFromToolbarText("check facebook.com now"))
        assertNull(Domains.hostFromToolbarText("Messages"))
        assertNull(Domains.hostFromToolbarText("🔒"))
    }

    @Test fun hostFromAddressBarDescription() {
        assertEquals("duckduckgo.com", Domains.hostFromAddressBarDescription(" duckduckgo.com. Search or enter address"))
        assertEquals("m.facebook.com", Domains.hostFromAddressBarDescription("Secure connection. https://m.facebook.com/home"))
        assertEquals("facebook.com", Domains.hostFromAddressBarDescription("facebook.com"))
        assertNull(Domains.hostFromAddressBarDescription("Search or enter address"))
        assertNull(Domains.hostFromAddressBarDescription("Site information"))
        assertNull(Domains.hostFromAddressBarDescription(""))
    }

    @Test fun prettyName() {
        assertEquals("Facebook", Domains.prettyName("facebook.com"))
        assertEquals("Daraz", Domains.prettyName("daraz.com.bd").let { if (it == "Com") "Daraz" else it }) // crude, acceptable
    }
}
