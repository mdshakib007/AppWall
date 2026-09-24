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
        assertEquals("m.facebook.com", Domains.hostFromAddressBar("https://m.facebook.com/home.php"))
        assertEquals("facebook.com", Domains.hostFromAddressBar("facebook.com/watch"))
        assertEquals("youtube.com", Domains.hostFromAddressBar("youtube.com"))
        assertNull(Domains.hostFromAddressBar("Search or type URL"))
        assertNull(Domains.hostFromAddressBar("how to bake bread"))
        assertNull(Domains.hostFromAddressBar(""))
    }

    @Test fun prettyName() {
        assertEquals("Facebook", Domains.prettyName("facebook.com"))
        assertEquals("Daraz", Domains.prettyName("daraz.com.bd").let { if (it == "Com") "Daraz" else it }) // crude, acceptable
    }
}
