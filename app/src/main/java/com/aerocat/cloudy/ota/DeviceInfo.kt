package com.aerocat.cloudy.ota

import android.os.Build
import java.io.File

/** Reads the *installed* device state so Tab 1 can compare it against the remote release. */
object DeviceInfo {
    val model: String get() = "${Build.MANUFACTURER} ${Build.MODEL}"
    val codename: String get() = Build.DEVICE               // "a32" on the A32 4G
    val androidVersion: String get() = Build.VERSION.RELEASE ?: "?"
    val securityPatch: String get() = Build.VERSION.SECURITY_PATCH
    val fingerprint: String get() = Build.FINGERPRINT
    val buildDisplay: String get() = Build.DISPLAY

    val kernelVersion: String get() = runCatching {
        File("/proc/version").readText().trim()
    }.getOrElse { System.getProperty("os.version") ?: "?" }

    /**
     * The ROM's own version stamp. LumiROM builds set this in the build props
     * (e.g. `ro.cloudy.version=8.6.4`). Reading it is the most reliable way to know
     * what's actually installed — more so than fingerprint diffing. Empty if unset
     * (e.g. Cloudy running on a non-LumiROM build).
     */
    val cloudyVersion: String get() = getProp("ro.cloudy.version").orEmpty()

    /** A-Only vs A/B, detected from the ROM slot suffix property. */
    val isAOnly: Boolean get() = runCatching {
        val slot = getProp("ro.boot.slot_suffix")
        slot.isNullOrEmpty()
    }.getOrDefault(true)

    fun getProp(key: String): String? = runCatching {
        val p = Runtime.getRuntime().exec(arrayOf("getprop", key))
        p.inputStream.bufferedReader().readLine()?.trim()
    }.getOrNull()
}
