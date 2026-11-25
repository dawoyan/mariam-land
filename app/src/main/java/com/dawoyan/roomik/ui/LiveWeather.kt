package com.dawoyan.roomik.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class LocalWeather(val temperature: Double, val wind: Double, val code: Int, val day: Boolean, val time: String) {
    val snow get() = code in 71..77 || code in 85..86
    val rain get() = code in 51..67 || code in 80..82 || code in 95..99
}

@Composable
fun WeatherParticles(weather: LocalWeather) {
    var frame by remember { mutableStateOf(0) }
    LaunchedEffect(weather) { while (true) { delay(60); frame++ } }
    Canvas(Modifier.fillMaxSize()) {
        for (i in 0 until 65) {
            val x = ((i * 79 + frame * weather.wind.coerceAtMost(50.0).toInt()) % size.width.toInt().coerceAtLeast(1)).toFloat()
            val y = ((i * 137 + frame * if (weather.snow) 3 else 16) % size.height.toInt().coerceAtLeast(1)).toFloat()
            if (weather.snow) drawCircle(Color.White.copy(alpha = .75f), 3f, Offset(x, y))
            else drawLine(Color(0x887BB9E7), Offset(x, y), Offset(x - 4, y + 19), 2f)
        }
    }
}

@Composable
fun LiveWeather(onWeather: (LocalWeather) -> Unit) {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    fun allowed() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    var permitted by remember { mutableStateOf(allowed()) }
    var location by remember { mutableStateOf<Location?>(null) }
    var weather by remember { mutableStateOf<LocalWeather?>(null) }
    var status by remember { mutableStateOf("Погода: разрешите геолокацию") }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permitted = allowed(); if (!permitted) status = "Геолокация не разрешена" }
    DisposableEffect(permitted) {
        val listener = LocationListener { location = it }
        if (permitted) try {
            val providers = manager.getProviders(true).filter { it == LocationManager.GPS_PROVIDER || it == LocationManager.NETWORK_PROVIDER }
            location = providers.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
            providers.forEach { manager.requestLocationUpdates(it, 60000L, 100f, listener, Looper.getMainLooper()) }
            if (location == null) status = "Определяем местоположение…"
        } catch (_: Exception) { status = "Местоположение недоступно" }
        onDispose { try { manager.removeUpdates(listener) } catch (_: Exception) {} }
    }
    LaunchedEffect(location?.latitude, location?.longitude) {
        val at = location ?: return@LaunchedEffect
        while (true) {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val url = URL("https://api.open-meteo.com/v1/forecast?latitude=${at.latitude}&longitude=${at.longitude}&current=temperature_2m,wind_speed_10m,weather_code,is_day&timezone=auto")
                    val conn = url.openConnection() as HttpURLConnection
                    try {
                        conn.connectTimeout = 10000; conn.readTimeout = 10000
                        val c = JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).getJSONObject("current")
                        LocalWeather(c.getDouble("temperature_2m"), c.getDouble("wind_speed_10m"), c.getInt("weather_code"), c.getInt("is_day") == 1, c.getString("time"))
                    } finally { conn.disconnect() }
                }
            }
            result.onSuccess { weather = it; onWeather(it); status = "" }.onFailure { status = "Погода недоступна — проверьте интернет" }
            delay(15 * 60 * 1000L)
        }
    }
    AssistChip(onClick = { if (!permitted) request.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }, label = {
        val w = weather
        Text(if (status.isNotEmpty()) status else if (w != null) "${if (w.snow) "❄️" else if (w.rain) "🌧️" else if (!w.day) "🌙" else "☀️"} ${w.temperature.toInt()}°C · 💨 ${w.wind.toInt()} км/ч · ${w.time.substringAfter('T')}" else "Погода…")
    })
}
