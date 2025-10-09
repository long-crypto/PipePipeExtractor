//package project.pipepipe.extractor.services.bilibili
//
//import com.fasterxml.jackson.databind.JsonNode
//import org.java_websocket.client.WebSocketClient
//import org.java_websocket.handshake.ServerHandshake
//import project.pipepipe.extractor.ExtractorContext
//import java.io.ByteArrayOutputStream
//import java.io.IOException
//import java.net.URI
//import java.net.URISyntaxException
//import java.nio.ByteBuffer
//import java.nio.charset.StandardCharsets
//import java.util.concurrent.Executors
//import java.util.concurrent.ScheduledExecutorService
//import java.util.concurrent.TimeUnit
//import java.util.concurrent.atomic.AtomicBoolean
//import kotlin.math.pow
//
//class BilibiliWebSocketClient(
//    private val id: Long,
//    private val token: String
//) {
//    var webSocketClient: WrappedWebSocketClient
//    private val shouldStop = AtomicBoolean(false)
//    val messages = mutableListOf<JsonNode>()
//
//    init {
//        webSocketClient = WrappedWebSocketClient()
//    }
//
//    inner class WrappedWebSocketClient : WebSocketClient(
//        URI("wss://broadcastlv.chat.bilibili.com/sub"),
//        BilibiliService.getWebSocketHeaders()
//    ) {
//        private var executor: ScheduledExecutorService? = null
//
//        init {
//            connectionLostTimeout = 0
//        }
//
//        @Throws(IOException::class)
//        fun encode(data: String, op: Int): ByteArray {
//            val dataByte = data.toByteArray(StandardCharsets.UTF_8)
//            val length = dataByte.size
//            val result = byteArrayOf(0, 0, 0, 0, 0, 16, 0, 1, 0, 0, 0, op.toByte(), 0, 0, 0, 1)
//
//            for (i in 0..3) {
//                result[i] = ((16 + length) / 256.0.pow(3 - i)).toInt().toByte()
//            }
//
//            return ByteArrayOutputStream().use { outputStream ->
//                outputStream.write(result)
//                outputStream.write(dataByte)
//                outputStream.toByteArray()
//            }
//        }
//
//        fun readInt(bytes: ByteArray, start: Int, len: Int): Int {
//            var result = 0
//            for (i in len - 1 downTo 0) {
//                result += (256.0.pow(len - i - 1) * bytes[start + i]).toInt()
//            }
//            return result
//        }
//
//        private fun authPacket(): String {
//            return """{"uid":0,"roomid":$id,"protover":3,"platform":"web","clientver":"1.4.0","type":2, "key":"$token"}"""
//        }
//
//        override fun onOpen(handshakedata: ServerHandshake) {
//            try {
//                send(encode(authPacket(), 7))
//            } catch (e: IOException) {
//                throw RuntimeException(e)
//            }
//
//            executor = Executors.newSingleThreadScheduledExecutor()
//            executor?.scheduleAtFixedRate({
//                try {
//                    while (!shouldStop.get()) {
//                        send(encode("", 2))
//                    }
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }, 30000, 30000, TimeUnit.MILLISECONDS)
//        }
//
//        override fun onMessage(message: String) {
//            // Empty implementation
//        }
//
//        override fun onMessage(byteBuffer: ByteBuffer) {
//            val data = byteBuffer.array()
//            val op = readInt(data, 8, 4)
//            if (op != 5) {
//                return
//            }
//
//            val type = readInt(data, 6, 2)
//            val body = data.sliceArray(16 until data.size)
//
//            val rawJson = when (type) {
//                0 -> String(body)
//                2 -> String(Utils.decompressZlib(body))
//                3 -> try {
//                    Utils.decompressBrotli(body)
//                } catch (e: IOException) {
//                    throw RuntimeException(e)
//                }
//                else -> return
//            }
//
//            val regex = "[\\x00-\\x1f]+"
//            val groups = rawJson.split(regex.toRegex())
//
//            for (group in groups) {
//                try {
//                    val result = ExtractorContext.objectMapper.readTree(group)
//                    messages.add(result)
//                } catch (ignored: Exception) {
//                    // Ignore parsing errors
//                }
//            }
//        }
//
//        override fun onClose(code: Int, reason: String, remote: Boolean) {
//            println(code)
//            if (code != -1 && !shouldStop.get()) {
//                try {
//                    wrappedReconnect()
//                } catch (e: URISyntaxException) {
//                    throw RuntimeException(e)
//                } catch (e: InterruptedException) {
//                    throw RuntimeException(e)
//                }
//            }
//        }
//
//        override fun onError(ex: Exception) {
//            println(ex.fillInStackTrace())
//        }
//
//        fun stopTimer() {
//            try {
//                executor?.shutdown()
//            } catch (ignored: Exception) {
//                // Ignore shutdown errors
//            }
//        }
//    }
//
//    @Throws(URISyntaxException::class, InterruptedException::class)
//    fun wrappedReconnect() {
//        webSocketClient.stopTimer()
//        webSocketClient = WrappedWebSocketClient()
//        webSocketClient.connectBlocking()
//    }
//
//    fun getCurrentMessagesAndClearList(): List<JsonNode> {
//        val temp = messages.toList()
//        messages.clear()
//        return temp
//    }
//
//    fun disconnect() {
//        shouldStop.set(true)
//        webSocketClient.closeConnection(-1, "Scheduled terminate")
//        webSocketClient.stopTimer()
//    }
//}
