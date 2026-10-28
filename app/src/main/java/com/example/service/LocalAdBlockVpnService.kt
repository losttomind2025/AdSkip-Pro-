package com.example.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.AdSkipApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class LocalAdBlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var workerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_CONNECT
        if (action == ACTION_DISCONNECT) {
            stopVpn()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (_isVpnActive.value) return

        try {
            val builder = Builder()
                .setSession("AdBlock Shield")
                .setMtu(1500)
                .addAddress("10.10.10.2", 32)
                // AdGuard Official Ad-blocking DNS
                .addDnsServer(PRIMARY_DNS)
                .addDnsServer(SECONDARY_DNS)
                // Route DNS traffic through our local TUN filter
                .addRoute(PRIMARY_DNS, 32)
                .addRoute(SECONDARY_DNS, 32)

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                _isVpnActive.value = true
                startDnsPacketLoop()
                Log.d(TAG, "AdBlock VPN Service established successfully")
            } else {
                Log.e(TAG, "Failed to establish VPN interface (null)")
                _isVpnActive.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AdBlock VPN", e)
            _isVpnActive.value = false
        }
    }

    private fun startDnsPacketLoop() {
        workerJob?.cancel()
        val pfd = vpnInterface ?: return

        workerJob = serviceScope.launch {
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val outputStream = FileOutputStream(pfd.fileDescriptor)
            val packet = ByteBuffer.allocate(32767)

            // Dedicated protected socket for upstream DNS lookups
            var upstreamSocket: DatagramSocket? = null
            try {
                upstreamSocket = DatagramSocket()
                protect(upstreamSocket)
                upstreamSocket.soTimeout = 2000
            } catch (e: Exception) {
                Log.e(TAG, "Error creating protected upstream socket", e)
            }

            while (isActive && _isVpnActive.value) {
                try {
                    packet.clear()
                    val length = inputStream.read(packet.array())
                    if (length > 0) {
                        packet.limit(length)
                        handleIncomingPacket(packet, outputStream, upstreamSocket)
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                    // Avoid busy looping on empty or interrupted read
                    try { kotlinx.coroutines.delay(10) } catch (_: Exception) {}
                }
            }

            try { upstreamSocket?.close() } catch (_: Exception) {}
        }
    }

    private fun handleIncomingPacket(
        packet: ByteBuffer,
        outputStream: FileOutputStream,
        upstreamSocket: DatagramSocket?
    ) {
        val array = packet.array()
        val length = packet.limit()

        // Minimum IPv4 (20) + UDP (8) + DNS Header (12) = 40 bytes
        if (length < 40) return

        // Verify IPv4 & UDP protocol (17)
        val ipVersionAndIhl = array[0].toInt() and 0xFF
        if ((ipVersionAndIhl ushr 4) != 4) return
        val ihl = (ipVersionAndIhl and 0x0F) * 4
        if (ihl < 20 || ihl >= length) return

        val protocol = array[9].toInt() and 0xFF
        if (protocol != 17) return // Not UDP

        // UDP Header starts after IP header
        val srcPort = ((array[ihl].toInt() and 0xFF) shl 8) or (array[ihl + 1].toInt() and 0xFF)
        val dstPort = ((array[ihl + 2].toInt() and 0xFF) shl 8) or (array[ihl + 3].toInt() and 0xFF)
        if (dstPort != 53) return // Not standard DNS

        val dnsOffset = ihl + 8
        if (dnsOffset + 12 > length) return

        val domain = extractDomainFromDns(array, dnsOffset + 12, length)

        if (isAdDomain(domain)) {
            // Block ad at network root!
            Log.d(TAG, "BLOCKED AD DOMAIN: $domain")
            try {
                AdSkipApplication.instance.preferences.recordNetworkAdBlocked(domain)
            } catch (_: Exception) {}

            // Send synthesized 0.0.0.0 NX/Null response back to TUN
            val blockedResponse = createBlockedDnsResponse(array, ihl, dnsOffset, length)
            if (blockedResponse != null) {
                try {
                    outputStream.write(blockedResponse)
                    outputStream.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing blocked response to TUN", e)
                }
            }
        } else {
            // Forward valid query to upstream ad-blocking DNS (AdGuard / Cloudflare)
            if (upstreamSocket != null && !upstreamSocket.isClosed) {
                try {
                    val dnsPayloadLength = length - dnsOffset
                    val forwardPacket = DatagramPacket(
                        array, dnsOffset, dnsPayloadLength,
                        InetAddress.getByName(PRIMARY_DNS), 53
                    )
                    upstreamSocket.send(forwardPacket)

                    val responseBuffer = ByteArray(2048)
                    val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                    upstreamSocket.receive(responsePacket)

                    // Reconstruct IPv4 + UDP packet with response
                    val resolvedPacket = createIpUdpPacket(
                        originalPacket = array,
                        ihl = ihl,
                        dnsResponsePayload = responseBuffer,
                        responseLength = responsePacket.length
                    )
                    if (resolvedPacket != null) {
                        outputStream.write(resolvedPacket)
                        outputStream.flush()
                    }
                } catch (e: Exception) {
                    // Fallback or timeout on UDP
                }
            }
        }
    }

    private fun extractDomainFromDns(data: ByteArray, startOffset: Int, maxLen: Int): String {
        var offset = startOffset
        val sb = StringBuilder()

        while (offset < maxLen) {
            val len = data[offset].toInt() and 0xFF
            if (len == 0) break // End of QNAME
            if ((len and 0xC0) == 0xC0) {
                // Pointer compression (not typical in initial query, stop)
                break
            }
            offset++
            if (offset + len > maxLen) break

            if (sb.isNotEmpty()) sb.append(".")
            for (i in 0 until len) {
                sb.append(data[offset + i].toInt().toChar())
            }
            offset += len
        }
        return sb.toString().lowercase()
    }

    private fun isAdDomain(domain: String): Boolean {
        if (domain.isBlank()) return false
        for (pattern in KNOWN_AD_PATTERNS) {
            if (domain.contains(pattern)) {
                return true
            }
        }
        return false
    }

    private fun createBlockedDnsResponse(
        original: ByteArray,
        ihl: Int,
        dnsOffset: Int,
        totalLen: Int
    ): ByteArray? {
        val dnsLen = totalLen - dnsOffset
        if (dnsLen < 12) return null

        // Build DNS Answer with 0.0.0.0 (Null IP)
        // Header (12) + Question (dnsLen - 12) + Answer (16 bytes: ptr 2, type 2, class 2, ttl 4, len 2, ip 4)
        val answerLen = 16
        val newDnsLen = dnsLen + answerLen
        val newTotalLen = ihl + 8 + newDnsLen
        val out = ByteArray(newTotalLen)

        // Copy IP Header
        System.arraycopy(original, 0, out, 0, ihl)
        // Swap IPs in IPv4 header
        System.arraycopy(original, 16, out, 12, 4) // Source = original Dest
        System.arraycopy(original, 12, out, 16, 4) // Dest = original Source
        // Set new Total Length in IPv4
        out[2] = (newTotalLen ushr 8).toByte()
        out[3] = (newTotalLen and 0xFF).toByte()

        // UDP Header
        val udpOffset = ihl
        out[udpOffset] = original[udpOffset + 2] // Src port = Dest port
        out[udpOffset + 1] = original[udpOffset + 3]
        out[udpOffset + 2] = original[udpOffset]     // Dest port = Src port
        out[udpOffset + 3] = original[udpOffset + 1]
        val newUdpLen = 8 + newDnsLen
        out[udpOffset + 4] = (newUdpLen ushr 8).toByte()
        out[udpOffset + 5] = (newUdpLen and 0xFF).toByte()
        out[udpOffset + 6] = 0 // Checksum (optional in IPv4 UDP)
        out[udpOffset + 7] = 0

        // Copy original DNS Header & Question
        System.arraycopy(original, dnsOffset, out, udpOffset + 8, dnsLen)
        val outDns = udpOffset + 8

        // Modify DNS Header flags: 0x8180 (Standard Query Response, No Error)
        out[outDns + 2] = 0x81.toByte()
        out[outDns + 3] = 0x80.toByte()
        // Answers Count = 1
        out[outDns + 6] = 0x00.toByte()
        out[outDns + 7] = 0x01.toByte()

        // Append Answer Resource Record:
        val ansOffset = outDns + dnsLen
        out[ansOffset] = 0xC0.toByte()      // Pointer to question name (0x0C)
        out[ansOffset + 1] = 0x0C.toByte()
        out[ansOffset + 2] = 0x00.toByte()  // Type A (0x0001)
        out[ansOffset + 3] = 0x01.toByte()
        out[ansOffset + 4] = 0x00.toByte()  // Class IN (0x0001)
        out[ansOffset + 5] = 0x01.toByte()
        out[ansOffset + 6] = 0x00.toByte()  // TTL: 300 seconds
        out[ansOffset + 7] = 0x00.toByte()
        out[ansOffset + 8] = 0x01.toByte()
        out[ansOffset + 9] = 0x2C.toByte()
        out[ansOffset + 10] = 0x00.toByte() // Data length: 4 bytes
        out[ansOffset + 11] = 0x04.toByte()
        out[ansOffset + 12] = 0x00.toByte() // IP: 0.0.0.0
        out[ansOffset + 13] = 0x00.toByte()
        out[ansOffset + 14] = 0x00.toByte()
        out[ansOffset + 15] = 0x00.toByte()

        // Recalculate IPv4 Checksum
        calculateIpv4Checksum(out, ihl)
        return out
    }

    private fun createIpUdpPacket(
        originalPacket: ByteArray,
        ihl: Int,
        dnsResponsePayload: ByteArray,
        responseLength: Int
    ): ByteArray? {
        val totalLength = ihl + 8 + responseLength
        val out = ByteArray(totalLength)

        System.arraycopy(originalPacket, 0, out, 0, ihl)
        System.arraycopy(originalPacket, 16, out, 12, 4) // Src = old Dst
        System.arraycopy(originalPacket, 12, out, 16, 4) // Dst = old Src
        out[2] = (totalLength ushr 8).toByte()
        out[3] = (totalLength and 0xFF).toByte()

        val udpOffset = ihl
        out[udpOffset] = originalPacket[udpOffset + 2]
        out[udpOffset + 1] = originalPacket[udpOffset + 3]
        out[udpOffset + 2] = originalPacket[udpOffset]
        out[udpOffset + 3] = originalPacket[udpOffset + 1]
        val udpLen = 8 + responseLength
        out[udpOffset + 4] = (udpLen ushr 8).toByte()
        out[udpOffset + 5] = (udpLen and 0xFF).toByte()
        out[udpOffset + 6] = 0
        out[udpOffset + 7] = 0

        System.arraycopy(dnsResponsePayload, 0, out, udpOffset + 8, responseLength)
        calculateIpv4Checksum(out, ihl)
        return out
    }

    private fun calculateIpv4Checksum(data: ByteArray, ihl: Int) {
        data[10] = 0
        data[11] = 0
        var sum = 0
        for (i in 0 until ihl step 2) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
        }
        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val checksum = (sum.inv()) and 0xFFFF
        data[10] = (checksum ushr 8).toByte()
        data[11] = (checksum and 0xFF).toByte()
    }

    private fun stopVpn() {
        workerJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
        _isVpnActive.value = false
        Log.d(TAG, "AdBlock VPN Service stopped")
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "LocalAdBlockVpn"
        const val ACTION_CONNECT = "com.example.service.CONNECT"
        const val ACTION_DISCONNECT = "com.example.service.DISCONNECT"

        const val PRIMARY_DNS = "94.140.14.14" // AdGuard DNS (Ad-blocking)
        const val SECONDARY_DNS = "94.140.14.15"

        private val _isVpnActive = MutableStateFlow(false)
        val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

        // Comprehensive Ad & Tracker Domain Signatures
        val KNOWN_AD_PATTERNS = listOf(
            "doubleclick",
            "googleads",
            "googlesyndication",
            "pagead",
            "adservice.google",
            "admob",
            "unityads",
            "unity3d.com/ads",
            "applovin",
            "adcolony",
            "vungle",
            "ironsrc",
            "taboola",
            "outbrain",
            "adnxs",
            "criteo",
            "rubiconproject",
            "pubmatic",
            "openx",
            "amazon-adsystem",
            "advertising.com",
            "chartboost",
            "flurry",
            "inmobi",
            "mopub",
            "popads",
            "propellerads",
            "adsterra",
            "exoclick",
            "trafficjunky",
            "scorecardresearch",
            "quantserve",
            "telemetry",
            "analytics"
        )
    }
}
