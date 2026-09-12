package com.network.speedtest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiquidGlassSpeedTestApp()
        }
    }
}

@Composable
fun LiquidGlassSpeedTestApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val netManager = remember { NetworkManager() }

    // پیش‌فرض تم روشن (Light Mode)
    var isDarkTheme by remember { mutableStateOf(false) }
    var isEnglish by remember { mutableStateOf(false) }
    var isByteMode by remember { mutableStateOf(false) }

    var testStarted by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var liveSpeedMbps by remember { mutableStateOf(0.0) }

    var ping by remember { mutableStateOf("-") }
    var jitter by remember { mutableStateOf("-") }
    var finalDownloadMbps by remember { mutableStateOf(0.0) }
    var finalUploadMbps by remember { mutableStateOf(0.0) }

    var ip by remember { mutableStateOf("-") }
    var location by remember { mutableStateOf("-") }
    var isp by remember { mutableStateOf("-") }
    var dnsStatus by remember { mutableStateOf("-") }
    var dnsServers by remember { mutableStateOf("-") }

    val unitMultiplier = if (isByteMode) 0.125 else 1.0
    val unitText = if (isByteMode) "MB/s" else "Mbps"

    fun formatSpeed(mbps: Double): String {
        return if (mbps <= 0.0) "0.00" else String.format("%.2f", mbps * unitMultiplier)
    }

    // پالت شیشه‌ای تم روشن و تاریک
    val baseBg = if (isDarkTheme) Color(0xFF070B14) else Color(0xFFF1F5F9)
    val textMain = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textMuted = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    val cyanAccent = Color(0xFF00E5FF)
    val purpleAccent = Color(0xFFA855F7)
    val greenAccent = Color(0xFF10B981)

    // انیمیشن تنفس نوری دکمه GO
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        // گوی‌های نوری افکت شیشه‌ای مایع
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-30).dp, y = 40.dp)
                .blur(90.dp)
                .background(if (isDarkTheme) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF38BDF8).copy(alpha = 0.45f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .blur(95.dp)
                .background(if (isDarkTheme) Color(0xFF7C3AED).copy(alpha = 0.25f) else Color(0xFFA855F7).copy(alpha = 0.35f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ردیف نوار بالای صفحه (شیشه‌ای)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPEEDTEST",
                    color = textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidPillChip(text = if (isByteMode) "MB/s" else "Mbps", isDark = isDarkTheme, activeColor = cyanAccent) {
                        isByteMode = !isByteMode
                    }
                    LiquidPillChip(text = if (isEnglish) "FA" else "EN", isDark = isDarkTheme, activeColor = purpleAccent) {
                        isEnglish = !isEnglish
                    }
                    LiquidPillChip(text = if (isDarkTheme) "☀️" else "🌙", isDark = isDarkTheme, activeColor = textMain) {
                        isDarkTheme = !isDarkTheme
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // صفحه شروع (وقتی هنوز GO زده نشده: فقط دکمه تپ GO نمایش داده می‌شود)
            if (!testStarted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((190 * pulseScale).dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        cyanAccent.copy(alpha = if (isDarkTheme) 0.18f else 0.25f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(175.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.7f)
                                    else Color.White.copy(alpha = 0.85f)
                                )
                                .border(
                                    2.5.dp,
                                    Brush.linearGradient(listOf(cyanAccent, purpleAccent)),
                                    CircleShape
                                )
                                .clickable {
                                    scope.launch {
                                        testStarted = true
                                        isTesting = true
                                        finalDownloadMbps = 0.0
                                        finalUploadMbps = 0.0
                                        liveSpeedMbps = 0.0
                                        ping = "..."
                                        jitter = "..."
                                        dnsStatus = if (isEnglish) "Checking..." else "در حال بررسی..."

                                        // ۱. مشخصات آی‌پی و لوکیشن
                                        val ipInfo = netManager.getIpDetails()
                                        ip = ipInfo["ip"] ?: "-"
                                        val flag = ipInfo["flag"] ?: ""
                                        val city = ipInfo["city"] ?: "-"
                                        val country = ipInfo["country"] ?: "-"
                                        location = if (flag.isNotEmpty()) "$flag $city, $country" else "$city, $country"
                                        isp = ipInfo["isp"] ?: "-"

                                        // ۲. پینگ و جیتر
                                        val pingRes = netManager.measurePingAndJitter()
                                        ping = if (pingRes.latency >= 0) "${pingRes.latency}" else "Timeout"
                                        jitter = "${pingRes.jitter}"

                                        // ۳. سنجش دانلود
                                        val dSpeed = netManager.testDownloadSpeed { cur ->
                                            liveSpeedMbps = cur
                                            finalDownloadMbps = cur
                                        }
                                        finalDownloadMbps = dSpeed
                                        liveSpeedMbps = 0.0

                                        // ۴. سنجش آپلود
                                        val uSpeed = netManager.testUploadSpeed { cur ->
                                            liveSpeedMbps = cur
                                            finalUploadMbps = cur
                                        }
                                        finalUploadMbps = uSpeed
                                        liveSpeedMbps = 0.0

                                        // ۵. بررسی نشتی DNS
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
                                color = textMain,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp
                            )
                        }
                    }
                }
            } else {
                // نمایش بعد از زدن دکمه GO
                AnimatedVisibility(
                    visible = testStarted,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        // کارت‌های تفکیک شده دانلود و آپلود
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LiquidCard(modifier = Modifier.weight(1f), isDark = isDarkTheme) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⤓", color = cyanAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEnglish) "Download" else "دانلود", color = textMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatSpeed(finalDownloadMbps),
                                        color = cyanAccent,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(unitText, color = textMuted, fontSize = 11.sp)
                                }
                            }

                            LiquidCard(modifier = Modifier.weight(1f), isDark = isDarkTheme) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⤒", color = purpleAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEnglish) "Upload" else "آپلود", color = textMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatSpeed(finalUploadMbps),
                                        color = purpleAccent,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(unitText, color = textMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // کارت پینگ و Jitter
                        LiquidCard(modifier = Modifier.fillMaxWidth(), isDark = isDarkTheme) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Ping:", color = textMuted, fontSize = 13.sp)
                                    Text("$ping ms", color = greenAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (isEnglish) "Jitter (Variance):" else "نوسان پینگ (Jitter):", color = textMuted, fontSize = 13.sp)
                                    Text("$jitter ms", color = cyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // کیلومترشمار زنده به همراه عقربه چرخشی و اعداد مدرج
                        AnalogSpeedometerGauge(
                            speed = liveSpeedMbps * unitMultiplier,
                            unit = unitText,
                            textColor = textMain,
                            subColor = textMuted,
                            accentColor = cyanAccent
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (!isTesting) {
                            TextButton(onClick = { testStarted = false }) {
                                Text(if (isEnglish) "⟳ Test Again" else "⟳ تست مجدد", color = cyanAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // اطلاعات شبکه و DNS
                        LiquidCard(modifier = Modifier.fillMaxWidth(), isDark = isDarkTheme) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(if (isEnglish) "Network Identity" else "اطلاعات شبکه", color = textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                RowItem(if (isEnglish) "IP Address" else "آدرس IP", ip, textMuted, textMain)
                                RowItem(if (isEnglish) "Location" else "موقعیت", location, textMuted, textMain)
                                RowItem(if (isEnglish) "ISP" else "ارائه‌دهنده", isp, textMuted, textMain)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = textMuted.copy(alpha = 0.15f))
                                RowItem(if (isEnglish) "DNS Leak Status" else "وضعیت نشت DNS", dnsStatus, textMuted, textMain)
                                if (dnsServers != "-") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(dnsServers, color = textMain, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // دکمه کپی
                        LiquidCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable {
                                    val report = """
                                        📊 SpeedTest Diagnostics:
                                        📥 Download: ${formatSpeed(finalDownloadMbps)} $unitText
                                        📤 Upload: ${formatSpeed(finalUploadMbps)} $unitText
                                        ⚡ Ping: $ping ms | Jitter: $jitter ms
                                        🌐 IP: $ip
                                        📍 Location: $location ($isp)
                                        🛡 DNS Leak: $dnsStatus
                                    """.trimIndent()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Diagnostics", report))
                                    Toast.makeText(context, if (isEnglish) "Copied!" else "گزارش کپی شد", Toast.LENGTH_SHORT).show()
                                },
                            isDark = isDarkTheme
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (isEnglish) "Copy Diagnostic Report" else "کپی نتایج به کلیپ‌بورد", color = textMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

// رسم کیلومترشمار آنالوگ با درجه‌بندی عددی و عقربه چرخان
@Composable
fun AnalogSpeedometerGauge(
    speed: Double,
    unit: String,
    textColor: Color,
    subColor: Color,
    accentColor: Color
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 150f),
        label = "needle_speed"
    )

    // سرعت حداکثر کیلومترشمار = 100
    val maxDisplaySpeed = 100f
    val sweepAngle = 260f
    val startAngle = 140f
    val progress = (animatedSpeed / maxDisplaySpeed).coerceIn(0f, 1f)
    val needleAngle = startAngle + (sweepAngle * progress)

    Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val arcSize = Size(size.width - strokeWidth * 2, size.height - strokeWidth * 2)
            val topLeft = Offset(strokeWidth, strokeWidth)

            // کمان پس‌زمینه
            drawArc(
                color = subColor.copy(alpha = 0.15f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // کمان رنگی میزان سرعت
            drawArc(
                brush = Brush.sweepGradient(listOf(accentColor.copy(alpha = 0.3f), accentColor)),
                startAngle = startAngle,
                sweepAngle = sweepAngle * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // رسم درجات عددی (0, 10, 25, 50, 75, 100)
            val marks = listOf(0, 10, 25, 50, 75, 100)
            val textPaint = android.graphics.Paint().apply {
                color = subColor.hashCode()
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }

            marks.forEach { mark ->
                val ratio = mark / 100f
                val angleRad = (startAngle + ratio * sweepAngle) * (PI / 180f)
                val radius = (size.width / 2) - strokeWidth * 2.2f
                val x = (size.width / 2) + radius * cos(angleRad).toFloat()
                val y = (size.height / 2) + radius * sin(angleRad).toFloat() + 4.dp.toPx()
                drawContext.canvas.nativeCanvas.drawText(mark.toString(), x, y, textPaint)
            }

            // رسم عقربه فیزیکی چرخشی (Needle)
            val center = Offset(size.width / 2, size.height / 2)
            rotate(degrees = needleAngle - 180f, pivot = center) {
                // بدنه عقربه
                drawLine(
                    brush = Brush.linearGradient(listOf(accentColor, Color.White)),
                    start = center,
                    end = Offset(center.x - 70.dp.toPx(), center.y),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // گره مرکزی عقربه
            drawCircle(color = accentColor, radius = 6.dp.toPx(), center = center)
            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = center)
        }

        // متن دیجیتالی سرعت در وسط-پایین
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 55.dp)
        ) {
            Text(
                text = String.format("%.2f", speed),
                color = textColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(text = unit, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// کارت اختصاصی شیشه‌ای مایع
@Composable
fun LiquidCard(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val bgBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1E293B).copy(alpha = 0.65f),
                Color(0xFF0F172A).copy(alpha = 0.40f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                Color.White.copy(alpha = 0.55f)
            )
        )
    }

    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color.Black.copy(alpha = 0.06f)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

@Composable
fun LiquidPillChip(text: String, isDark: Boolean, activeColor: Color, onClick: () -> Unit) {
    val bg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val border = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = activeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RowItem(title: String, value: String, subColor: Color, mainColor: Color) {
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
