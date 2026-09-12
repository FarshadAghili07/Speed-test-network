package com.network.speedtest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ModernSpeedTestApp()
        }
    }
}

@Composable
fun ModernSpeedTestApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val netManager = remember { NetworkManager() }

    // پیش‌فرض تم روشن (غیر دارک)
    var isDarkTheme by remember { mutableStateOf(false) }
    var isEnglish by remember { mutableStateOf(false) }
    // انتخاب واحد: مگابیت (false) یا مگابایت (true)
    var isByteMode by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var testStarted by remember { mutableStateOf(false) }
    var liveSpeedMbps by remember { mutableStateOf(0.0) }

    var ping by remember { mutableStateOf("-") }
    var finalDownloadMbps by remember { mutableStateOf(0.0) }
    var finalUploadMbps by remember { mutableStateOf(0.0) }

    var ip by remember { mutableStateOf("-") }
    var location by remember { mutableStateOf("-") }
    var isp by remember { mutableStateOf("-") }
    var dnsStatus by remember { mutableStateOf("-") }
    var dnsServers by remember { mutableStateOf("-") }

    // تم‌ها
    val bgColor = if (isDarkTheme) Color(0xFF0D111A) else Color(0xFFF6F8FA)
    val cardBg = if (isDarkTheme) Color(0xFF161C28) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color(0xFFF3F4F6) else Color(0xFF111827)
    val subTextColor = if (isDarkTheme) Color(0xFF8C9AA9) else Color(0xFF6B7280)
    val cyanAccent = Color(0xFF00E5FF)
    val purpleAccent = Color(0xFF8B5CF6)
    val greenAccent = Color(0xFF10B981)

    // ضریب تبدیل واحد سرعت
    val unitMultiplier = if (isByteMode) 0.125 else 1.0
    val unitText = if (isByteMode) "MB/s" else "Mbps"

    fun formatSpeed(mbps: Double): String {
        return if (mbps <= 0.0) "0.0" else String.format("%.2f", mbps * unitMultiplier)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // ردیف نوار بالا (تنظیمات: تم، زبان، سوییچ واحد)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPEEDTEST",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // سوییچ Mbps / MB/s
                    PillChip(
                        text = if (isByteMode) "MB/s" else "Mbps",
                        activeColor = cyanAccent,
                        textColor = textColor,
                        bgColor = cardBg,
                        onClick = { isByteMode = !isByteMode }
                    )
                    // سوییچ زبان
                    PillChip(
                        text = if (isEnglish) "FA" else "EN",
                        activeColor = purpleAccent,
                        textColor = textColor,
                        bgColor = cardBg,
                        onClick = { isEnglish = !isEnglish }
                    )
                    // سوییچ تم
                    PillChip(
                        text = if (isDarkTheme) "☀️" else "🌙",
                        activeColor = textColor,
                        textColor = textColor,
                        bgColor = cardBg,
                        onClick = { isDarkTheme = !isDarkTheme }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // باکس‌های Download و Upload
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // دانلود
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⤓", color = cyanAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEnglish) "Download" else "دانلود", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatSpeed(finalDownloadMbps),
                            color = cyanAccent,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(unitText, color = subTextColor, fontSize = 11.sp)
                    }
                }

                // آپلود
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⤒", color = purpleAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEnglish) "Upload" else "آپلود", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatSpeed(finalUploadMbps),
                            color = purpleAccent,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(unitText, color = subTextColor, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // نوار باریک Ping
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isEnglish) "Ping / Latency" else "پینگ (تأخیر)", color = subTextColor, fontSize = 13.sp)
                Text("$ping ms", color = greenAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // بخش مرکزی: دکمه بزرگ GO یا گیج آنالوگ زنده
            Box(
                modifier = Modifier
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!testStarted) {
                    // دکمه بزرگ دایره‌ای GO
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(CircleShape)
                            .border(3.dp, cyanAccent, CircleShape)
                            .clickable(enabled = !isTesting) {
                                scope.launch {
                                    isTesting = true
                                    testStarted = true
                                    finalDownloadMbps = 0.0
                                    finalUploadMbps = 0.0
                                    liveSpeedMbps = 0.0
                                    ping = "..."
                                    dnsStatus = if (isEnglish) "Checking..." else "در حال بررسی..."

                                    // ۱. اطلاعات شبکه
                                    val ipInfo = netManager.getIpDetails()
                                    ip = ipInfo["ip"] ?: "-"
                                    val flag = ipInfo["flag"] ?: ""
                                    val city = ipInfo["city"] ?: "-"
                                    val country = ipInfo["country"] ?: "-"
                                    location = if (flag.isNotEmpty()) "$flag $city, $country" else "$city, $country"
                                    isp = ipInfo["isp"] ?: "-"

                                    // ۲. پینگ
                                    val p = netManager.measurePing()
                                    ping = if (p >= 0) "$p" else "Timeout"

                                    // ۳. تست دانلود
                                    val dSpeed = netManager.testDownloadSpeed { current ->
                                        liveSpeedMbps = current
                                        finalDownloadMbps = current
                                    }
                                    finalDownloadMbps = dSpeed
                                    liveSpeedMbps = 0.0

                                    // ۴. تست آپلود
                                    val uSpeed = netManager.testUploadSpeed { current ->
                                        liveSpeedMbps = current
                                        finalUploadMbps = current
                                    }
                                    finalUploadMbps = uSpeed
                                    liveSpeedMbps = 0.0

                                    // ۵. تست نشت دی‌ان‌اس
                                    val (status, servers) = netManager.checkDnsLeak(ip)
                                    dnsStatus = status
                                    dnsServers = servers

                                    isTesting = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "GO",
                            color = textColor,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                } else {
                    // نمایش گیج سرعت و عقربه
                    SpeedometerView(
                        currentSpeed = liveSpeedMbps * unitMultiplier,
                        unit = unitText,
                        textColor = textColor,
                        accentColor = cyanAccent,
                        subColor = subTextColor
                    )
                }
            }

            if (testStarted && !isTesting) {
                TextButton(onClick = { testStarted = false }) {
                    Text(if (isEnglish) "Test Again" else "تست مجدد", color = cyanAccent, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // کارت جزئیات اتصال
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if (isEnglish) "Network Identity" else "اطلاعات شبکه", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    SpeedDetailRow(if (isEnglish) "IP Address" else "آدرس IP", ip, subTextColor, textColor)
                    SpeedDetailRow(if (isEnglish) "Location" else "موقعیت", location, subTextColor, textColor)
                    SpeedDetailRow(if (isEnglish) "Provider" else "ارائه‌دهنده", isp, subTextColor, textColor)
                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = subTextColor.copy(alpha = 0.2f))
                    SpeedDetailRow(if (isEnglish) "DNS Leak" else "وضعیت نشت DNS", dnsStatus, subTextColor, textColor)
                    if (dnsServers != "-") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(dnsServers, color = textColor, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // دکمه کپی
            OutlinedButton(
                onClick = {
                    val report = """
                        📊 SpeedTest Report:
                        📥 Download: ${formatSpeed(finalDownloadMbps)} $unitText
                        📤 Upload: ${formatSpeed(finalUploadMbps)} $unitText
                        ⚡ Ping: $ping ms
                        🌐 IP: $ip
                        📍 Location: $location ($isp)
                        🛡 DNS Leak: $dnsStatus
                    """.trimIndent()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("SpeedTest", report))
                    Toast.makeText(context, if (isEnglish) "Copied!" else "گزارش کپی شد", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEnglish) "Copy Report" else "کپی نتایج به کلیپ‌بورد", color = subTextColor)
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// ویجت رسم گیج آنالوگ و عقربه
@Composable
fun SpeedometerView(
    currentSpeed: Double,
    unit: String,
    textColor: Color,
    accentColor: Color,
    subColor: Color
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed.toFloat(),
        animationSpec = tween(300),
        label = "speed"
    )

    // محاسبه زاویه چرخش (از ۱۴۰ درجه تا ۴۰۰ درجه)
    val maxSpeed = 100f
    val progress = (animatedSpeed / maxSpeed).coerceIn(0f, 1f)
    val startAngle = 140f
    val sweepAngle = 260f

    Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // کمان پس‌زمینه خاکستری
            drawArc(
                color = subColor.copy(alpha = 0.2f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // کمان رنگی میزان سرعت
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(accentColor.copy(alpha = 0.4f), accentColor)
                ),
                startAngle = startAngle,
                sweepAngle = sweepAngle * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // متن عدد سرعت در وسط
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.2f", currentSpeed),
                color = textColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = unit,
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PillChip(text: String, activeColor: Color, textColor: Color, bgColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, activeColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SpeedDetailRow(title: String, value: String, subColor: Color, mainColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = subColor, fontSize = 13.sp)
        Text(value, color = mainColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
