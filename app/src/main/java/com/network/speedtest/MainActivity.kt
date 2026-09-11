package com.network.speedtest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeedTestApp()
        }
    }
}

@Composable
fun SpeedTestApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val netManager = remember { NetworkManager() }

    var isDarkTheme by remember { mutableStateOf(true) }
    var isEnglish by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf("") }
    var ping by remember { mutableStateOf("-") }
    var downloadSpeed by remember { mutableStateOf("0.0") }
    var uploadSpeed by remember { mutableStateOf("0.0") }
    var ip by remember { mutableStateOf("-") }
    var location by remember { mutableStateOf("-") }
    var isp by remember { mutableStateOf("-") }
    var dnsStatus by remember { mutableStateOf("-") }
    var dnsServers by remember { mutableStateOf("-") }

    val bgColor = if (isDarkTheme) Color(0xFF090D16) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkTheme) Color(0xFF131B2E) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
    val borderColor = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val accentDownload = Color(0xFF0284C7)
    val accentUpload = Color(0xFF8B5CF6)
    val accentPing = Color(0xFF10B981)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ردیف سربرگ با دکمه‌های تغییر تم و زبان
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnglish) "Network Insight" else "سنجش هوشمند شبکه",
                    color = textColor,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { isEnglish = !isEnglish },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = cardBg,
                            contentColor = textColor
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "FA" else "EN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = { isDarkTheme = !isDarkTheme },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = cardBg,
                            contentColor = textColor
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = if (isDarkTheme) "☀️ روز" else "🌙 شب",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // بخش تفکیک‌شده دانلود و آپلود
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEnglish) "DOWNLOAD" else "دانلود",
                            color = subTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = downloadSpeed,
                            color = accentDownload,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mbps",
                            color = subTextColor,
                            fontSize = 12.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEnglish) "UPLOAD" else "آپلود",
                            color = subTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uploadSpeed,
                            color = accentUpload,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mbps",
                            color = subTextColor,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // پینگ
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Latency / Ping" else "زمان رفت و برگشت (Ping)",
                        color = subTextColor,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "$ping ms",
                        color = accentPing,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // مشخصات اتصال همراه با پرچم
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isEnglish) "Network Identity" else "مشخصات شبکه و اتصال",
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoRow(title = if (isEnglish) "IP Address" else "آدرس IP", value = ip, subColor = subTextColor, textColor = textColor)
                    InfoRow(title = if (isEnglish) "Location" else "موقعیت جغرافیایی", value = location, subColor = subTextColor, textColor = textColor)
                    InfoRow(title = if (isEnglish) "Provider" else "ارائه‌دهنده (ISP)", value = isp, subColor = subTextColor, textColor = textColor)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // بررسی نشت دی‌ان‌اس
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isEnglish) "DNS Leak Integrity" else "امنیت و نشت DNS",
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoRow(title = if (isEnglish) "Status" else "وضعیت نشت", value = dnsStatus, subColor = subTextColor, textColor = textColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isEnglish) "Active Resolvers:" else "تحلیل‌کننده‌های فعال:",
                        color = subTextColor,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dnsServers,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // دکمه اجرای ترتیبی تست‌ها
            Button(
                onClick = {
                    scope.launch {
                        isTesting = true
                        downloadSpeed = "..."
                        uploadSpeed = "-"
                        ping = "..."
                        dnsStatus = if (isEnglish) "Analyzing..." else "در حال بررسی..."

                        val ipInfo = netManager.getIpDetails()
                        ip = ipInfo["ip"] ?: "-"
                        val flag = ipInfo["flag"] ?: ""
                        val city = ipInfo["city"] ?: "-"
                        val country = ipInfo["country"] ?: "-"
                        location = if (flag.isNotEmpty()) "$flag $city, $country" else "$city, $country"
                        isp = ipInfo["isp"] ?: "-"

                        val p = netManager.measurePing()
                        ping = if (p >= 0) "$p" else "Timeout"

                        currentStep = if (isEnglish) "Testing Download..." else "در حال سنجش دانلود..."
                        val dSpeed = netManager.testDownloadSpeed { current: Double ->
                            downloadSpeed = current.toString()
                        }
                        downloadSpeed = String.format("%.1f", dSpeed)

                        uploadSpeed = "..."
                        currentStep = if (isEnglish) "Testing Upload..." else "در حال سنجش آپلود..."
                        val uSpeed = netManager.testUploadSpeed { current: Double ->
                            uploadSpeed = current.toString()
                        }
                        uploadSpeed = String.format("%.1f", uSpeed)

                        currentStep = if (isEnglish) "Checking DNS Leak..." else "بررسی نشت دی‌ان‌اس..."
                        val (status, servers) = netManager.checkDnsLeak(ip)
                        dnsStatus = status
                        dnsServers = servers

                        isTesting = false
                        currentStep = ""
                    }
                },
                enabled = !isTesting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(
                    text = if (isTesting) (if (currentStep.isNotEmpty()) currentStep else "...") else (if (isEnglish) "Run Full Diagnostics" else "شروع تست کامل"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    val report = if (isEnglish) {
                        """
                        Network Diagnostic Report:
                        - Download: $downloadSpeed Mbps
                        - Upload: $uploadSpeed Mbps
                        - Ping: $ping ms
                        - IP: $ip
                        - Location: $location ($isp)
                        - DNS Status: $dnsStatus
                        - Resolvers: $dnsServers
                        """.trimIndent()
                    } else {
                        """
                        گزارش تست شبکه:
                        - دانلود: $downloadSpeed Mbps
                        - آپلود: $uploadSpeed Mbps
                        - پینگ: $ping ms
                        - آی‌پی: $ip
                        - موقعیت: $location ($isp)
                        - وضعیت نشت DNS: $dnsStatus
                        - سرورها: $dnsServers
                        """.trimIndent()
                    }

                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("NetReport", report))
                    Toast.makeText(
                        context,
                        if (isEnglish) "Report copied to clipboard" else "گزارش در کلیپ‌بورد کپی شد",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = subTextColor)
            ) {
                Text(
                    text = if (isEnglish) "Copy Diagnostics" else "کپی نتایج به کلیپ‌بورد",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun InfoRow(title: String, value: String, subColor: Color, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = subColor, fontSize = 13.sp)
        Text(value, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
