package com.paperdesk.companion

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object Paths {
    const val REMOTE_ROOT = "/sdcard/Vishwakarma"
    const val REMOTE_BUNDLE = "$REMOTE_ROOT/os-bundle.json"
    const val REMOTE_INBOX = "$REMOTE_ROOT/inbox/os-bundle.json"
    const val REMOTE_SETTINGS = "$REMOTE_ROOT/inbox/settings.json"
    const val REMOTE_WEATHER = "$REMOTE_ROOT/weather.json"
    const val REMOTE_WEATHER_INBOX = "$REMOTE_ROOT/inbox/weather.json"
    const val REMOTE_DEVICE = "$REMOTE_ROOT/device.json"
    const val REMOTE_SYNC_REQUEST = "$REMOTE_ROOT/sync-request.json"
    const val REMOTE_PROJECTS = "$REMOTE_ROOT/Projects"

    fun cacheDir(): File {
        val d = File(System.getProperty("user.home"), ".paperdesk-companion")
        d.mkdirs()
        return d
    }

    fun localBundle(): File = File(cacheDir(), "os-bundle.json")
}

object MeasureMath {
    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    fun kind(length: String, breadth: String, height: String, auto: Boolean, stored: String): String {
        if (!auto) return stored
        return when {
            filled(height) -> "VOL"
            filled(breadth) -> "AREA"
            else -> "RUN"
        }
    }

    fun qty(length: String, breadth: String, height: String, kind: String): Double {
        val l = length.toDoubleOrNull() ?: 0.0
        val b = breadth.toDoubleOrNull() ?: 0.0
        val h = height.toDoubleOrNull() ?: 0.0
        return when (kind) {
            "RUN" -> l
            "VOL" -> l * b * h
            else -> l * b
        }
    }

    fun unit(kind: String, uom: String): String {
        val base = uom.ifBlank { "m" }
        return when (kind) {
            "RUN" -> base
            "VOL" -> base + "3"
            else -> base + "2"
        }
    }

    fun label(length: String, breadth: String, height: String, uom: String, auto: Boolean, stored: String): String {
        val k = kind(length, breadth, height, auto, stored)
        return "${fmt.format(qty(length, breadth, height, k))} ${unit(k, uom)}"
    }

    fun totals(rows: JSONArray): String {
        var runM = 0.0
        var areaM2 = 0.0
        var volM3 = 0.0
        for (i in 0 until rows.length()) {
            val o = rows.optJSONObject(i) ?: continue
            val k = kind(o.optString("length"), o.optString("breadth"), o.optString("height"), o.optBoolean("autoKind", true), o.optString("kind", "AREA"))
            val q = qty(o.optString("length"), o.optString("breadth"), o.optString("height"), k)
            val m = when (o.optString("uom", "M")) {
                "MM" -> 0.001
                "CM" -> 0.01
                "FT" -> 0.3048
                "IN" -> 0.0254
                else -> 1.0
            }
            when (k) {
                "RUN" -> runM += q * m
                "AREA" -> areaM2 += q * m * m
                else -> volM3 += q * m * m * m
            }
        }
        val parts = mutableListOf<String>()
        if (runM != 0.0) parts += "${fmt.format(runM)} m"
        if (areaM2 != 0.0) parts += "${fmt.format(areaM2)} m2"
        if (volM3 != 0.0) parts += "${fmt.format(volM3)} m3"
        return if (parts.isEmpty()) "" else parts.joinToString("   ")
    }

    private fun filled(s: String) = (s.toDoubleOrNull() ?: 0.0) != 0.0 || s.isNotBlank()
}

fun emptyBundle(): JSONObject = JSONObject()
    .put("version", 1)
    .put("os", "AcharyaOS")
    .put("osVersion", "2.0")
    .put("projects", JSONArray())
    .put("schedules", JSONArray())
    .put("todos", JSONArray())
    .put("notes", JSONArray())
    .put("measurements", JSONArray())
    .put("alarms", JSONArray())
    .put("calculatorHistory", JSONArray())
    .put(
        "settings",
        JSONObject()
            .put("pages", JSONArray(listOf("desk", "schedule", "grantha")))
            .put("landscape", true)
            .put("theme", "paper")
            .put("accent", "#FF6A00")
            .put("statusBar", false)
            .put("navBar", false)
            .put("alwaysOn", false)
            .put("clockAnalog", true)
            .put("projectEnabled", false)
            .put("measureEnabled", false)
            .put("calcEnabled", false)
            .put("todoEnabled", false)
            .put("scheduleEnabled", true)
            .put("notesEnabled", false)
            .put("gitaEnabled", false)
            .put("notifications", true)
            .put("usbDebug", true)
            .put("developerOptions", true)
    )

fun JSONArray.objects(): List<JSONObject> =
    (0 until length()).mapNotNull { optJSONObject(it) }

fun projectName(bundle: JSONObject, id: String): String {
    if (id.isBlank()) return ""
    bundle.optJSONArray("projects")?.objects()?.firstOrNull { it.optString("id") == id }?.let {
        return it.optString("name")
    }
    return ""
}

fun architectureProfile(): JSONObject = JSONObject()
    .put("name", "Acharya Mode")
    .put("landscape", true)
    .put("theme", "paper")
    .put("notifications", false)
    .put("projectEnabled", false)
    .put("measureEnabled", false)
    .put("calcEnabled", false)
    .put("todoEnabled", false)
    .put("scheduleEnabled", true)
    .put("notesEnabled", false)
    .put("gitaEnabled", false)
    .put("pages", org.json.JSONArray(listOf("desk", "schedule", "grantha")))
    .put("sound", false)
    .put("vibration", false)
    .put("lockScreen", false)
