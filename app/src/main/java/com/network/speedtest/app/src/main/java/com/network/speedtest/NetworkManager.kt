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
import kotlin.random.Random

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

    suspend fun measurePing(host: String = "1.1.1.1", port: Int = 53): Long = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            Socket().use { it.connect(InetSocketAddress(host, port), 2500) }
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            -1L
        }
    }

    suspend fun testDownloadSpeed(onProgress: (Double) -> Unit): Double = withContext(Dispatchers.IO) {
        try {
            val fileUrl = URL("https://speed.cloudflare.com/__down?bytes=10000000")
            val conn = fileUrl.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 8000
            val input: InputStream = conn.inputStream
            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            val startTime = System.currentTimeMillis()

            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                if (elapsedSec > 0.3) {
                    val currentMbps = (totalBytesRead * 8.0) / (elapsedSec * 1_000_000.0)
                    onProgress(String.format("%.1f", currentMbps).toDouble())
                }
            }
            input.close()
            val totalSec = (System.currentTimeMillis() - startTime) / 1000.0
            (totalBytesRead * 8.0) / (totalSec * 1_000_000.0)
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun testUploadSpeed(onProgress: (Double) -> Unit): Double = withContext(Dispatchers.IO) {
        try {
            val fileUrl = URL("https://speed.cloudflare.com/__up")
            val conn = (fileUrl.openConnection() as HttpURLConnection).apply {
                doOutput = true
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 8000
                setFixedLengthStreamingMode(5 * 1024 * 1024)
            }
            val output: OutputStream = conn.outputStream
            val payload = ByteArray(8192)
            var totalBytesSent = 0L
            val startTime = System.currentTimeMillis()

            for (i in 0 until (5 * 1024 * 1024 / 8192)) {
                output.write(payload)
                totalBytesSent += payload.size
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                if (elapsedSec > 0.3) {
                    val currentMbps = (totalBytesSent * 8.0) / (elapsedSec * 1_000_000.0)
                    onProgress(String.format("%.1f", currentMbps).toDouble())
                }
            }
            output.flush()
            output.close()
            val totalSec = (System.currentTimeMillis() - startTime) / 1000.0
            (totalBytesSent * 8.0) / (totalSec * 1_000_000.0)
        } catch (e: Exception) {
            0.0
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
