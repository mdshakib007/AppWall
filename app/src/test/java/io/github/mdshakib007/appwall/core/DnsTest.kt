package io.github.mdshakib007.appwall.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DnsTest {
    private fun query(name: String, id: Int = 0x1234): ByteArray {
        val out = ArrayList<Byte>()
        out += (id shr 8).toByte(); out += id.toByte()
        out += 0x01; out += 0x00           // RD
        out += 0; out += 1                  // QD
        out += 0; out += 0; out += 0; out += 0; out += 0; out += 0
        name.split('.').forEach { l -> out += l.length.toByte(); out.addAll(l.toByteArray().toList()) }
        out += 0
        out += 0; out += 1                  // A
        out += 0; out += 1                  // IN
        return out.toByteArray()
    }

    @Test fun parsesQuestion() {
        val q = Dns.parseQuestion(query("M.Facebook.COM"))
        assertNotNull(q)
        assertEquals("m.facebook.com", q!!.name)
        assertEquals(1, q.type)
    }

    @Test fun rejectsResponsesAndGarbage() {
        val r = query("a.com").also { it[2] = 0x81.toByte() }
        assertNull(Dns.parseQuestion(r))
        assertNull(Dns.parseQuestion(ByteArray(5)))
    }

    @Test fun nxDomainKeepsIdAndQuestion() {
        val q = query("facebook.com", 0xBEEF)
        val r = Dns.buildNxDomain(q)
        assertEquals(0xBE.toByte(), r[0]); assertEquals(0xEF.toByte(), r[1])
        assertEquals(0x81.toByte(), r[2])          // QR + RD
        assertEquals(0x83.toByte(), r[3])          // RA + NXDOMAIN
        assertEquals(q.size, r.size)
        assertArrayEquals(q.copyOfRange(12, q.size), r.copyOfRange(12, r.size))
    }

    @Test fun ipv4UdpRoundTrip() {
        val payload = query("x.com")
        val src = byteArrayOf(10, 111, 222.toByte(), 1); val dst = byteArrayOf(10, 111, 222.toByte(), 3)
        val pkt = Dns.buildIpv4Udp(src, dst, 40000, 53, payload)
        val parsed = Dns.parseIpv4Udp(pkt, pkt.size)
        assertNotNull(parsed)
        assertArrayEquals(src, parsed!!.srcIp); assertArrayEquals(dst, parsed.dstIp)
        assertEquals(40000, parsed.srcPort); assertEquals(53, parsed.dstPort)
        assertArrayEquals(payload, parsed.payload)
        // IPv4 header checksum must verify to zero when summed over the header
        var sum = 0L
        for (i in 0 until 20 step 2) sum += ((pkt[i].toInt() and 0xFF) shl 8) or (pkt[i + 1].toInt() and 0xFF)
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        assertEquals(0xFFFFL, sum)
    }

    @Test fun ignoresTcp() {
        val pkt = Dns.buildIpv4Udp(byteArrayOf(1, 1, 1, 1), byteArrayOf(2, 2, 2, 2), 1, 53, ByteArray(20))
        pkt[9] = 6 // TCP
        assertNull(Dns.parseIpv4Udp(pkt, pkt.size))
    }
}
