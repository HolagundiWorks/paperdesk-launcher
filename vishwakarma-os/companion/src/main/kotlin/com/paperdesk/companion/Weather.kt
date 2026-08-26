package com.paperdesk.companion

import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object WeatherSync {
    private const val URL_SPEC =
        "https://api.open-meteo.com/v1/forecast?latitude=15.2693&longitude=76.3871" +
            "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day" +
            "&daily=temperature_2m_max,temperature_2m_min&timezone=Asia%2FKolkata"

    private var cache: JSONObject? = null
    private var cacheAt = 0L

    fun fetch(force: Boolean = false): JSONObject? {
        val now = System.currentTimeMillis()
        if (!force && cache != null && now - cacheAt < 15 * 60_000L) return cache
        return try {
            val conn = URL(URL_SPEC).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            if (conn.responseCode != 200) {
                conn.disconnect()
                return cache
            }
            val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            conn.disconnect()
            snapshot(JSONObject(body))?.also {
                cache = it
                cacheAt = now
            } ?: cache
        } catch (_: Exception) {
            cache
        }
    }

    fun push(bundle: JSONObject): String? {
        val weather = fetch() ?: return null
        bundle.put("weather", weather)
        val local = File(Paths.cacheDir(), "weather.json")
        local.writeText(weather.toString(2))
        Adb.shell("mkdir -p ${Paths.REMOTE_ROOT}/inbox")
        Adb.push(local, Paths.REMOTE_WEATHER)
        Adb.push(local, Paths.REMOTE_WEATHER_INBOX)
        Adb.broadcast("com.paperdesk.os.APPLY_WEATHER")
        return weather.optString("condition")
    }

    private fun snapshot(src: JSONObject): JSONObject? {
        return try {
            val cur = src.getJSONObject("current")
            val daily = src.getJSONObject("daily")
            val code = cur.getInt("weather_code")
            val isDay = cur.optInt("is_day", 1) == 1
            val now = System.currentTimeMillis()
            JSONObject()
                .put("source", "usb")
                .put("city", "Hospet, Karnataka")
                .put("tempC", cur.getDouble("temperature_2m"))
                .put("highC", daily.getJSONArray("temperature_2m_max").optDouble(0))
                .put("lowC", daily.getJSONArray("temperature_2m_min").optDouble(0))
                .put("humidity", cur.getInt("relative_humidity_2m"))
                .put("windKmh", cur.getDouble("wind_speed_10m"))
                .put("condition", condition(code, isDay))
                .put("kind", kind(code))
                .put("isDay", isDay)
                .put("updatedAt", now)
        } catch (_: Exception) {
            null
        }
    }

    private fun kind(code: Int) = when (code) {
        0, 1 -> "SUN"
        2 -> "CLOUD_SUN"
        3 -> "CLOUD"
        45, 48 -> "FOG"
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "RAIN"
        71, 73, 75, 77, 85, 86 -> "SNOW"
        95, 96, 99 -> "STORM"
        else -> "CLOUD"
    }

    private fun condition(code: Int, isDay: Boolean): String {
        if (!isDay && (code == 0 || code == 1)) return "Clear night"
        return when (code) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            80, 81, 82 -> "Rain showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Hospet"
        }
    }
}
