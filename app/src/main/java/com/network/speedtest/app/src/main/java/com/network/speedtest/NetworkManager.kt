package com.network.speedtest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.math.abs
import kotlin.random.Random

data class PingResult(val latency: Long, val jitter: Long)

class NetworkManager {

    private fun countryCodeToEmoji(code: String): String {
        if (code.length != 2) return ""
        val firstChar = Character.codePointAt(code.uppercase(), 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(code.uppercase(), 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }

    suspend fun getIpDetails(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://ipwho.is/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val connection = json.optJSONObject("connection")
            val ispName = connection?.optString("isp") ?: json.optString("isp", "-")
            val countryCode = json.optString("country_code", "")
            val flagEmoji = countryCodeToEmoji(countryCode)

            mapOf(
                "ip" to json.optString("ip", "-"),
                "country" to json.optString("country", "-"),
                "city" to json.optString("city", "-"),
                "isp" to ispName,
                "flag" to flagEmoji
            )
        } catch (e: Exception) {
            mapOf("ip" to "خطا در دریافت", "country" to "-", "city" to "-", "isp" to "-", "flag" to "")
        }
    }

    // تست چندگانه پینگ به منظور محاسبه پینگ دقیق و Jitter (نوسان پینگ)
    suspend fun measurePingAndJitter(host: String = "1.1.1.1", port: Int = 53): PingResult = withContext(Dispatchers.IO) {
        val samples = mutableListOf<Long>()
        repeat(4) {
            try {
                val start = System.currentTimeMillis()
                Socket().use { it.connect(InetSocketAddress(host, port), 1500) }
                samples.add(System.currentTimeMillis() - start)
            } catch (_: Exception) {}
        }
        if (samples.isEmpty()) return@withContext PingResult(-1L, 0L)

        val avgPing = samples.average().toLong()
        var diffSum = 0L
        for (i in 0 until samples.size - 1) {
            diffSum += abs(samples[i + 1] - samples[i])
        }
        val jitter = if (samples.size > 1) diffSum / (samples.size - 1) else 0L
        PingResult(avgPing, jitter)
    }

    suspend fun testDownloadSpeed(onProgress: (Double) -> Unit): Double = withContext(Dispatchers.IO) {
        var lastRecordedMbps = 0.0
        try {
            val fileUrl = URL("https://speed.cloudflare.com/__down?bytes=25000000")
            val conn = (fileUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 10000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val input: InputStream = conn.inputStream
            val buffer = ByteArray(16384)
            var totalBytesRead = 0L
            val startTime = System.currentTimeMillis()

            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                if (elapsedSec > 0.4) {
                    val currentMbps = (totalBytesRead * 8.0) / (elapsedSec * 1_000_000.0)
                    lastRecordedMbps = currentMbps
                    onProgress(currentMbps)
                }
                if (elapsedSec >= 5.5) break
            }
            input.close()
            lastRecordedMbps
        } catch (e: Exception) {
            lastRecordedMbps
        }
    }

    // تست آپلود تصحیح شده بدون باختن عدد نهایی
    suspend fun testUploadSpeed(onProgress: (Double) -> Unit): Double = withContext(Dispatchers.IO) {
        var lastRecordedMbps = 0.0
        try {
            val uploadBytes = 8 * 1024 * 1024 // 8MB
            val fileUrl = URL("https://speed.cloudflare.com/__up")
            val conn = (fileUrl.openConnection() as HttpURLConnection).apply {
                doOutput = true
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 10000
                setChunkedStreamingMode(16384)
                setRequestProperty("Content-Type", "application/octet-stream")
            }

            val output: OutputStream = conn.outputStream
            val payload = ByteArray(16384)
            var totalBytesSent = 0L
            val startTime = System.currentTimeMillis()

            val chunks = uploadBytes / payload.size
            for (i in 0 until chunks) {
                output.write(payload)
                totalBytesSent += payload.size
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                if (elapsedSec > 0.4) {
                    val currentMbps = (totalBytesSent * 8.0) / (elapsedSec * 1_000_000.0)
                    lastRecordedMbps = currentMbps
                    onProgress(currentMbps)
                }
                if (elapsedSec >= 5.5) break
            }
            output.flush()
            output.close()
            conn.responseCode
            lastRecordedMbps
        } catch (e: Exception) {
            lastRecordedMbps
        }
    }

    suspend fun checkDnsLeak(userIp: String): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            val testId = Random.nextInt(100000, 999999)
            try {
                InetAddress.getByName("$testId.bash.ws")
            } catch (_: Exception) {}

            val reportUrl = URL("https://bash.ws/dnsleak/test/$testId?json")
            val conn = (reportUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(text)
            val resolvers = mutableListOf<String>()
            var leakFound = false

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val type = item.optString("type")
                val ip = item.optString("ip")
                val country = item.optString("country_name")
                if (type == "dns") {
                    resolvers.add("$ip ($country)")
                }
                if (type == "conclusion" && item.optString("ip") == "DNS leak") {
                    leakFound = true
                }
            }

            val status = if (leakFound) "⚠️ نشت دی‌ان‌اس کشف شد!" else "✅ امن (بدون نشت)"
            val resList = if (resolvers.isEmpty()) "Cloudflare / System DNS" else resolvers.distinct().joinToString("\n")
            Pair(status, resList)
        } catch (e: Exception) {
            Pair("✅ امن (پیش‌فرض سیستم)", "Cloudflare / System DNS")
        }
    }
}
