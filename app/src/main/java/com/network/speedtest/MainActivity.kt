package com.network.speedtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val scope = rememberCoroutineScope()
    val netManager = remember { NetworkManager() }

    var isTesting by remember { mutableStateOf(false) }
    var ping by remember { mutableStateOf("-") }
    var speed by remember { mutableStateOf("0.0") }
    var ip by remember { mutableStateOf("-") }
    var location by remember { mutableStateOf("-") }
    var isp by remember { mutableStateOf("-") }
    var dnsStatus by remember { mutableStateOf("-") }
    var dnsServers by remember { mutableStateOf("-") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "سنجش شبکه و امنیت DNS",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // کارت سرعت و پینگ
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("سرعت دانلود", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    Text(
                        text = "$speed Mbps",
                        color = Color(0xFF38BDF8),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "پینگ: $ping ms",
                        color = if (ping != "-") Color(0xFF4ADE80) else Color(0xFF94A3B8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // کارت مشخصات اتصال و IP
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("مشخصات اتصال", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    RowDetail(title = "آدرس IP:", value = ip)
                    RowDetail(title = "موقعیت:", value = location)
                    RowDetail(title = "ارائه‌دهنده (ISP):", value = isp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // کارت وضعیت DNS Leak
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("وضعیت نشت DNS (Leak Test)", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    RowDetail(title = "نتیجه:", value = dnsStatus)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("سرورهای تحلیل‌کننده:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text(dnsServers, color = Color.White, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    scope.launch {
                        isTesting = true
                        speed = "..."
                        ping = "..."
                        dnsStatus = "در حال تحلیل..."

                        val ipInfo = netManager.getIpDetails()
                        ip = ipInfo["ip"] ?: "-"
                        location = "${ipInfo["city"]}, ${ipInfo["country"]}"
                        isp = ipInfo["isp"] ?: "-"

                        val p = netManager.measurePing()
                        ping = if (p >= 0) "$p" else "تایم‌اوت"

                        val finalSpeed = netManager.testDownloadSpeed { current ->
                            speed = current.toString()
                        }
                        speed = String.format("%.1f", finalSpeed)

                        val (status, servers) = netManager.checkDnsLeak(ip)
                        dnsStatus = status
                        dnsServers = servers

                        isTesting = false
                    }
                },
                enabled = !isTesting,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(
                    text = if (isTesting) "در حال تست..." else "شروع تست کامل",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun RowDetail(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = Color(0xFF94A3B8), fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

