package id.ac.usm.sipandutambak.data.remote.mqtt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.UUID

/**
 * Lightweight pure-Kotlin MQTT 3.1.1 TCP subscriber for real-time 3-second ESP32 telemetry.
 */
class MqttTelemetryClient(
    private val brokerHost: String = DEFAULT_BROKER_HOST,
    private val brokerPort: Int = DEFAULT_BROKER_PORT,
    private val topicFilter: String = DEFAULT_TELEMETRY_TOPIC
) {
    companion object {
        const val DEFAULT_BROKER_HOST = "192.168.100.41"
        const val DEFAULT_BROKER_PORT = 1883
        const val DEFAULT_TELEMETRY_TOPIC = "sipandu/devices/+/telemetry"
    }

    private var socket: Socket? = null
    private var readerJob: Job? = null

    fun start(
        scope: CoroutineScope,
        onConnectionChanged: (Boolean) -> Unit = {},
        onTelemetryMessage: (topic: String, payload: String) -> Unit
    ) {
        stop()
        readerJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                var currentSocket: Socket? = null
                try {
                    currentSocket = Socket().apply {
                        connect(InetSocketAddress(brokerHost, brokerPort), 5000)
                        soTimeout = 15000
                        tcpNoDelay = true
                    }
                    socket = currentSocket
                    val inp = DataInputStream(currentSocket.getInputStream())
                    val out = currentSocket.getOutputStream()

                    val clientId = "Android_Sipandu_${UUID.randomUUID().toString().take(8)}"
                    writePacket(out, 0x10, buildConnectBody(clientId))

                    val connAck = readPacket(inp)
                    if (connAck == null || (connAck.first ushr 4) != 2 || connAck.second.size < 2 || connAck.second[1].toInt() != 0) {
                        onConnectionChanged(false)
                        continue
                    }

                    writePacket(out, 0x82, buildSubscribeBody(packetId = 1, topic = topicFilter))
                    onConnectionChanged(true)

                    while (isActive && !currentSocket.isClosed) {
                        try {
                            val packet = readPacket(inp) ?: break
                            val packetType = packet.first ushr 4
                            val body = packet.second
                            if (packetType == 3 && body.size >= 2) {
                                val topicLen = ((body[0].toInt() and 0xFF) shl 8) or (body[1].toInt() and 0xFF)
                                if (2 + topicLen <= body.size) {
                                    val topic = String(body, 2, topicLen, Charsets.UTF_8)
                                    val qos = (packet.first ushr 1) and 0x03
                                    val payloadOffset = 2 + topicLen + if (qos > 0) 2 else 0
                                    if (payloadOffset <= body.size) {
                                        val jsonPayload = String(body, payloadOffset, body.size - payloadOffset, Charsets.UTF_8)
                                        onTelemetryMessage(topic, jsonPayload)
                                    }
                                }
                            }
                        } catch (_: SocketTimeoutException) {
                            // Keep connection alive on idle channel
                            writePacket(out, 0xC0, ByteArray(0))
                        }
                    }
                } catch (_: Exception) {
                    // Retry cleanly after delay
                } finally {
                    onConnectionChanged(false)
                    try {
                        currentSocket?.close()
                    } catch (_: Exception) {
                    }
                    socket = null
                }
                if (isActive) delay(5000)
            }
        }
    }

    fun stop() {
        readerJob?.cancel()
        readerJob = null
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }

    private fun buildConnectBody(clientId: String): ByteArray {
        val baos = ByteArrayOutputStream()
        writeUtf8(baos, "MQTT")
        baos.write(0x04) // MQTT 3.1.1
        baos.write(0x02) // Clean Session = 1
        baos.write(0x00)
        baos.write(30)   // KeepAlive = 30s
        writeUtf8(baos, clientId)
        return baos.toByteArray()
    }

    private fun buildSubscribeBody(packetId: Int, topic: String): ByteArray {
        val baos = ByteArrayOutputStream()
        baos.write((packetId ushr 8) and 0xFF)
        baos.write(packetId and 0xFF)
        writeUtf8(baos, topic)
        baos.write(0x00) // QoS 0
        return baos.toByteArray()
    }

    private fun writeUtf8(out: ByteArrayOutputStream, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        out.write((bytes.size ushr 8) and 0xFF)
        out.write(bytes.size and 0xFF)
        out.write(bytes)
    }

    private fun writePacket(out: OutputStream, headerByte: Int, body: ByteArray) {
        val packet = ByteArrayOutputStream()
        packet.write(headerByte)
        var x = body.size
        do {
            var encodedByte = x % 128
            x /= 128
            if (x > 0) encodedByte = encodedByte or 128
            packet.write(encodedByte)
        } while (x > 0)
        packet.write(body)
        out.write(packet.toByteArray())
        out.flush()
    }

    private fun readPacket(inp: DataInputStream): Pair<Int, ByteArray>? {
        val header = inp.read()
        if (header < 0) return null
        var multiplier = 1
        var remainingLength = 0
        var digit: Int
        do {
            digit = inp.read()
            if (digit < 0) return null
            remainingLength += (digit and 127) * multiplier
            multiplier *= 128
        } while ((digit and 128) != 0)
        val body = ByteArray(remainingLength)
        if (remainingLength > 0) {
            inp.readFully(body)
        }
        return header to body
    }
}
