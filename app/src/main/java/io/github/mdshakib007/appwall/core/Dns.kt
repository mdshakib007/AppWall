package io.github.mdshakib007.appwall.core

import java.nio.ByteBuffer

/**
 * Minimal IPv4 / UDP / DNS codec. Pure Kotlin; unit-tested.
 * We only ever look at the question section and answer with NXDOMAIN, so this stays tiny.
 */
object Dns {

    class UdpPacket(
        val srcIp: ByteArray, val dstIp: ByteArray,
        val srcPort: Int, val dstPort: Int,
        val payload: ByteArray,
    )

    /** Parses an IPv4 packet carrying UDP. Returns null for anything else (TCP, ICMP, IPv6, fragments...). */
    fun parseIpv4Udp(packet: ByteArray, length: Int): UdpPacket? {
        if (length < 28) return null
        val version = (packet[0].toInt() shr 4) and 0xF
        if (version != 4) return null
        val ihl = (packet[0].toInt() and 0xF) * 4
        if (ihl < 20 || length < ihl + 8) return null
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return null
        val flagsFrag = ((packet[6].toInt() and 0xFF) shl 8) or (packet[7].toInt() and 0xFF)
        if (flagsFrag and 0x1FFF != 0) return null // fragment, ignore
        val totalLen = ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF)
        val end = minOf(totalLen, length)
        val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        val udpLen = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)
        val payloadStart = ihl + 8
        val payloadEnd = minOf(ihl + udpLen, end)
        if (payloadEnd <= payloadStart) return null
        return UdpPacket(
            srcIp = packet.copyOfRange(12, 16), dstIp = packet.copyOfRange(16, 20),
            srcPort = srcPort, dstPort = dstPort,
            payload = packet.copyOfRange(payloadStart, payloadEnd),
        )
    }

    /** Builds an IPv4+UDP packet with correct checksums. */
    fun buildIpv4Udp(srcIp: ByteArray, dstIp: ByteArray, srcPort: Int, dstPort: Int, payload: ByteArray): ByteArray {
        val udpLen = 8 + payload.size
        val total = 20 + udpLen
        val b = ByteBuffer.allocate(total)
        b.put((0x45).toByte()); b.put(0)               // version/IHL, DSCP
        b.putShort(total.toShort())                     // total length
        b.putShort(0); b.putShort(0x4000.toShort())     // id, flags (DF)
        b.put(64); b.put(17); b.putShort(0)             // ttl, proto UDP, checksum placeholder
        b.put(srcIp); b.put(dstIp)
        b.putShort(srcPort.toShort()); b.putShort(dstPort.toShort())
        b.putShort(udpLen.toShort()); b.putShort(0)     // udp len, checksum placeholder
        b.put(payload)
        val arr = b.array()
        val ipSum = checksum(arr, 0, 20, 0L)
        arr[10] = (ipSum shr 8).toByte(); arr[11] = ipSum.toByte()
        // UDP pseudo header: src, dst, zero, proto, udp length
        var pseudo = 0L
        pseudo += ((srcIp[0].toInt() and 0xFF) shl 8) or (srcIp[1].toInt() and 0xFF)
        pseudo += ((srcIp[2].toInt() and 0xFF) shl 8) or (srcIp[3].toInt() and 0xFF)
        pseudo += ((dstIp[0].toInt() and 0xFF) shl 8) or (dstIp[1].toInt() and 0xFF)
        pseudo += ((dstIp[2].toInt() and 0xFF) shl 8) or (dstIp[3].toInt() and 0xFF)
        pseudo += 17
        pseudo += udpLen
        var udpSum = checksum(arr, 20, udpLen, pseudo)
        if (udpSum == 0) udpSum = 0xFFFF
        arr[26] = (udpSum shr 8).toByte(); arr[27] = udpSum.toByte()
        return arr
    }

    private fun checksum(data: ByteArray, offset: Int, length: Int, initial: Long): Int {
        var sum = initial
        var i = offset
        var remaining = length
        while (remaining > 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2; remaining -= 2
        }
        if (remaining == 1) sum += (data[i].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }

    class Question(val name: String, val type: Int, val qclass: Int)

    /** Returns the first question of a DNS message, or null if malformed / not a standard query. */
    fun parseQuestion(msg: ByteArray): Question? {
        if (msg.size < 12) return null
        val flags = ((msg[2].toInt() and 0xFF) shl 8) or (msg[3].toInt() and 0xFF)
        if (flags and 0x8000 != 0) return null // it's a response
        val qdCount = ((msg[4].toInt() and 0xFF) shl 8) or (msg[5].toInt() and 0xFF)
        if (qdCount < 1) return null
        val sb = StringBuilder()
        var i = 12
        var labels = 0
        while (true) {
            if (i >= msg.size) return null
            val len = msg[i].toInt() and 0xFF
            if (len == 0) { i++; break }
            if (len and 0xC0 != 0) return null // compression in question: not expected
            if (i + 1 + len > msg.size) return null
            if (labels > 0) sb.append('.')
            for (k in 0 until len) sb.append((msg[i + 1 + k].toInt() and 0xFF).toChar())
            i += 1 + len
            labels++
            if (labels > 127) return null
        }
        if (i + 4 > msg.size) return null
        val type = ((msg[i].toInt() and 0xFF) shl 8) or (msg[i + 1].toInt() and 0xFF)
        val qclass = ((msg[i + 2].toInt() and 0xFF) shl 8) or (msg[i + 3].toInt() and 0xFF)
        return Question(sb.toString().lowercase(), type, qclass)
    }

    /**
     * Builds an NXDOMAIN response for [query]: copies id + question, sets QR/RD/RA and RCODE=3.
     * Includes no records at all; resolvers negative-cache this briefly, which is what we want.
     */
    fun buildNxDomain(query: ByteArray): ByteArray {
        val qEnd = questionEnd(query) ?: 12
        val out = query.copyOfRange(0, qEnd)
        val rd = (query[2].toInt() and 0x01)
        out[2] = (0x80 or rd).toByte()     // QR=1, opcode 0, AA=0, TC=0, RD copied
        out[3] = (0x80 or 3).toByte()      // RA=1, Z=0, RCODE=3 NXDOMAIN
        out[4] = 0; out[5] = 1             // QDCOUNT = 1
        out[6] = 0; out[7] = 0             // ANCOUNT
        out[8] = 0; out[9] = 0             // NSCOUNT
        out[10] = 0; out[11] = 0           // ARCOUNT (drops any EDNS OPT; fine)
        return out
    }

    /** SERVFAIL (RCODE 2) with the same shape as [buildNxDomain]; clients retry / fail fast instead of hanging. */
    fun buildServFail(query: ByteArray): ByteArray = buildNxDomain(query).also { it[3] = (0x80 or 2).toByte() }

    private fun questionEnd(msg: ByteArray): Int? {
        var i = 12
        while (true) {
            if (i >= msg.size) return null
            val len = msg[i].toInt() and 0xFF
            if (len == 0) { i++; break }
            i += 1 + len
        }
        return if (i + 4 <= msg.size) i + 4 else null
    }
}
