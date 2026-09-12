package com.network.speedtest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import kotlin.math.ln
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeedTestNetworkApp()
        }
    }
}

@Composable
fun SpeedTestNetworkApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val netManager = remember { NetworkManager() }

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

    // رنگ‌بندی یکدست و مینیمال (بدون لکه‌های رنگی محو)
    val baseBg = if (isDarkTheme) Color(0xFF0E131F) else Color(0xFFF4F6F9)
    val cardBg = if (isDarkTheme) Color(0xFF171F30) else Color(0xFFFFFFFF)
    val textMain = if (isDarkTheme) Color(0xFFF9FAFB) else Color(0xFF111827)
    val textMuted = if (isDarkTheme) Color(0xFF8E9BAE) else Color(0xFF6B7280)
    val cardBorder = if (isDarkTheme) Color(0xFF26334D) else Color(0xFFE5E7EB)

    val cyanColor = Color(0xFF00C8E5)
    val purpleColor = Color(0xFFA855F7)
    val greenColor = Color(0xFF10B981)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_border"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = baseBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // هدر بالا
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPEEDTEST",
                    color = textMain,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniPillChip(text = if (isByteMode) "MB/s" else "Mbps", isDark = isDarkTheme, activeColor = cyanColor) {
                        isByteMode = !isByteMode
                    }
                    MiniPillChip(text = if (isEnglish) "FA" else "EN", isDark = isDarkTheme, activeColor = purpleColor) {
                        isEnglish = !isEnglish
                    }
                    MiniPillChip(text = if (isDarkTheme) "☀️" else "🌙", isDark = isDarkTheme, activeColor = textMain) {
                        isDarkTheme = !isDarkTheme
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // قبل از شروع تست: فقط دکمه GO دقیقاً مانند برنامه مرجع
            if (!testStarted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(490.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(pulseBorder.dp, cyanColor, CircleShape)
                            .clickable {
                                scope.launch {
                                    testStarted = true
                                    isTesting = true
                                    finalDownloadMbps = 0.0
                                    finalUploadMbps = 0.0
                                    liveSpeedMbps = 0.0
                                    ping = "..."
                                    jitter = "..."
                                    dnsStatus = if (isEnglish) "Auditing..." else "در حال بررسی..."

                                    val ipInfo = netManager.getIpDetails()
                                    ip = ipInfo["ip"] ?: "-"
                                    val flag = ipInfo["flag"] ?: ""
                                    val city = ipInfo["city"] ?: "-"
                                    val country = ipInfo["country"] ?: "-"
                                    location = if (flag.isNotEmpty()) "$flag $city, $country" else "$city, $country"
                                    isp = ipInfo["isp"] ?: "-"

                                    val pingRes = netManager.measurePingAndJitter()
                                    ping = if (pingRes.latency >= 0) "${pingRes.latency}" else "Timeout"
                                    jitter = "${pingRes.jitter}"

                                    // تست دانلود
                                    val dSpeed = netManager.testDownloadSpeed { cur ->
                                        liveSpeedMbps = cur
                                        finalDownloadMbps = cur
                                    }
                                    finalDownloadMbps = dSpeed
                                    liveSpeedMbps = 0.0

                                    // تست آپلود
                                    val uSpeed = netManager.testUploadSpeed { cur ->
                                        liveSpeedMbps = cur
                                        finalUploadMbps = cur
                                    }
                                    finalUploadMbps = uSpeed
                                    liveSpeedMbps = 0.0

                                    // تست نشت DNS
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
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = testStarted,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        // کارت‌های دانلود و آپلود
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CardBox(modifier = Modifier.weight(1f), cardBg = cardBg, border = cardBorder) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⤓", color = cyanColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEnglish) "Download" else "دانلود", color = textMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatSpeed(finalDownloadMbps),
                                        color = cyanColor,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(unitText, color = textMuted, fontSize = 11.sp)
                                }
                            }

                            CardBox(modifier = Modifier.weight(1f), cardBg = cardBg, border = cardBorder) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⤒", color = purpleColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEnglish) "Upload" else "آپلود", color = textMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatSpeed(finalUploadMbps),
                                        color = purpleColor,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(unitText, color = textMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ردیف مجزا برای Ping و Jitter (بدون نوسان پینگ)
                        CardBox(modifier = Modifier.fillMaxWidth(), cardBg = cardBg, border = cardBorder) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Ping:", color = textMuted, fontSize = 13.sp)
                                    Text("$ping ms", color = greenColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Jitter:", color = textMuted, fontSize = 13.sp)
                                    Text("$jitter ms", color = cyanColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // کیلومترشمار دقیقاً مانند Ookla Speedtest
                        SpeedtestGauge(
                            speed = liveSpeedMbps * unitMultiplier,
                            unit = unitText,
                            textColor = textMain,
                            accentColor = cyanColor,
                            isDark = isDarkTheme
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!isTesting) {
                            TextButton(onClick = { testStarted = false }) {
                                Text(if (isEnglish) "⟳ Test Again" else "⟳ تست مجدد", color = cyanColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // اطلاعات شبکه
                        CardBox(modifier = Modifier.fillMaxWidth(), cardBg = cardBg, border = cardBorder) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(if (isEnglish) "Network Identity" else "اطلاعات شبکه", color = textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                RowItem(if (isEnglish) "IP Address" else "آدرس IP", ip, textMuted, textMain)
                                RowItem(if (isEnglish) "Location" else "موقعیت", location, textMuted, textMain)
                                RowItem(if (isEnglish) "ISP" else "ارائه‌دهنده", isp, textMuted, textMain)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = cardBorder)
                                RowItem(if (isEnglish) "DNS Leak Status" else "وضعیت نشت DNS", dnsStatus, textMuted, textMain)
                                if (dnsServers != "-") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(dnsServers, color = textMain, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // دکمه کپی
                        CardBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable {
                                    val report = """
                                        SpeedTest Report:
                                        Download: ${formatSpeed(finalDownloadMbps)} $unitText
                                        Upload: ${formatSpeed(finalUploadMbps)} $unitText
                                        Ping: $ping ms | Jitter: $jitter ms
                                        IP: $ip
                                        Location: $location ($isp)
                                        DNS: $dnsStatus
                                    """.trimIndent()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("SpeedTest", report))
                                    Toast.makeText(context, if (isEnglish) "Copied!" else "کپی شد", Toast.LENGTH_SHORT).show()
                                },
                            cardBg = cardBg,
                            border = cardBorder
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

// گیج حرفه‌ای با مقیاس و ظاهر مشابه Ookla
@Composable
fun SpeedtestGauge(
    speed: Double,
    unit: String,
    textColor: Color,
    accentColor: Color,
    isDark: Boolean
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 120f),
        label = "gauge_needle"
    )

    // محاسبه لگاریتمی درجه‌بندی سرعت شبیه اسپیدتست واقعی: 0, 5, 10, 50, 100, 250, 500, 750, 1k
    val scalePoints = listOf(0.0, 5.0, 10.0, 50.0, 100.0, 250.0, 500.0, 750.0, 1000.0)
    val scaleLabels = listOf("0", "5", "10", "50", "100", "250", "500", "750", "1k")

    fun getAngleForSpeed(v: Float): Float {
        val clamped = v.coerceIn(0f, 1000f)
        for (i in 0 until scalePoints.size - 1) {
            val p1 = scalePoints[i].toFloat()
            val p2 = scalePoints[i + 1].toFloat()
            if (clamped in p1..p2) {
                val fraction = (clamped - p1) / (p2 - p1)
                val stepAngle = 240f / (scalePoints.size - 1)
                return 150f + (i * stepAngle) + (fraction * stepAngle)
            }
        }
        return 150f + 240f
    }

    val currentAngle = getAngleForSpeed(animatedSpeed)
    val startAngle = 150f
    val sweepAngle = 240f
    val currentSweep = (currentAngle - startAngle).coerceAtLeast(0f)

    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.width / 2) - strokeWidth

            // دایره پس‌زمینه نیمه‌تیره سرعت‌سنج
            drawCircle(
                color = if (isDark) Color(0xFF131A28) else Color(0xFFE5E9F0),
                radius = radius + (strokeWidth / 2),
                center = center
            )

            val arcTopLeft = Offset(strokeWidth, strokeWidth)
            val arcSize = Size(size.width - strokeWidth * 2, size.height - strokeWidth * 2)

            // کمان پس‌زمینه تیره
            drawArc(
                color = if (isDark) Color(0xFF1E283D) else Color(0xFFD1D5DB),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // کمان نئونی سرعت
            if (currentSweep > 0) {
                drawArc(
                    color = accentColor,
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // نوشتن اعداد دقیق مقیاس دور سرعت‌سنج
            val textPaint = Paint().apply {
                color = (if (isDark) 0xFF8E9BAE else 0xFF6B7280).toInt()
                textSize = 10.sp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            val numPoints = scaleLabels.size
            for (i in 0 until numPoints) {
                val stepAngle = 150f + i * (240f / (numPoints - 1))
                val rad = stepAngle * (PI / 180f)
                val textRadius = radius - strokeWidth * 1.3f
                val tx = center.x + textRadius * cos(rad).toFloat()
                val ty = center.y + textRadius * sin(rad).toFloat() + 4.dp.toPx()
                drawContext.canvas.nativeCanvas.drawText(scaleLabels[i], tx, ty, textPaint)
            }

            // عقربه فیزیکی گرادیانی مانند اسپیدتست
            rotate(degrees = currentAngle, pivot = center) {
                val needlePath = Path().apply {
                    moveTo(0f, -4.dp.toPx())
                    lineTo(radius - 12.dp.toPx(), -1.dp.toPx())
                    lineTo(radius - 12.dp.toPx(), 1.dp.toPx())
                    lineTo(0f, 4.dp.toPx())
                    close()
                }

                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.translate(center.x, center.y)

                drawPath(
                    path = needlePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.95f))
                    )
                )
                drawContext.canvas.nativeCanvas.restore()
            }

            // نشانگر مرکز عقربه
            drawCircle(color = accentColor, radius = 5.dp.toPx(), center = center)
        }

        // متن عدد سرعت دیجیتال در مرکز متمایل به پایین
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 52.dp)
        ) {
            Text(
                text = String.format("%.2f", speed),
                color = textColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = unit,
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    cardBg: Color,
    border: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
fun MiniPillChip(text: String, isDark: Boolean, activeColor: Color, onClick: () -> Unit) {
    val bg = if (isDark) Color(0xFF1E283D) else Color(0xFFE5E7EB)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
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
