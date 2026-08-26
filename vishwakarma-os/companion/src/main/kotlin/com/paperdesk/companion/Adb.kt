package com.paperdesk.companion

import java.io.File
import java.util.concurrent.TimeUnit

object Adb {
    private fun binary(): String {
        val env = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (!env.isNullOrBlank()) {
            val win = File(env, "platform-tools/adb.exe")
            val unix = File(env, "platform-tools/adb")
            if (win.exists()) return win.absolutePath
            if (unix.exists()) return unix.absolutePath
        }
        val local = File(System.getProperty("user.home"), "AppData/Local/Android/Sdk/platform-tools/adb.exe")
        if (local.exists()) return local.absolutePath
        return "adb"
    }

    fun run(vararg args: String, timeoutSec: Long = 20): String {
        val cmd = listOf(binary()) + args.toList()
        val pb = ProcessBuilder(cmd)
        pb.redirectErrorStream(true)
        val p = pb.start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor(timeoutSec, TimeUnit.SECONDS)
        if (p.isAlive) {
            p.destroyForcibly()
            return out + "\n(timeout)"
        }
        return out.trim()
    }

    fun serial(): String? {
        val lines = run("devices").lines().drop(1).filter { it.contains("\tdevice") }
        return lines.firstOrNull()?.substringBefore("\t")
    }

    fun connected(): Boolean = serial() != null

    fun shell(cmd: String): String = run("shell", cmd)

    fun pull(remote: String, local: File): String {
        local.parentFile?.mkdirs()
        return run("pull", remote, local.absolutePath)
    }

    fun push(local: File, remote: String): String = run("push", local.absolutePath, remote)

    fun broadcast(action: String, extras: Map<String, String> = emptyMap()): String {
        val args = mutableListOf(
            "shell", "am", "broadcast",
            "-n", "com.paperdesk.launcher/.bridge.OsBridgeReceiver",
            "-a", action
        )
        extras.forEach { (k, v) ->
            args += listOf("--es", k, v)
        }
        return run(*args.toTypedArray())
    }
}
