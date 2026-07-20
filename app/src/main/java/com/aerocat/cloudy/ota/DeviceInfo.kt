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
     * The ROM's own version stamp: `ro.cloudy.rom.ver` (e.g. "8.6.4").
     * Most reliable signal for what's installed - better than fingerprint diffing.
     * Empty when unset (e.g. Cloudy running on a non-LumiROM build).
     */
    val romVersion: String get() = getProp(PROP_ROM_VER).orEmpty()

    /** Optional numeric companion for clean integer comparison. */
    val romVersionCode: Long? get() = getProp(PROP_ROM_VER_CODE)?.toLongOrNull()

    /** Maintainer name baked into the ROM: `ro.cloudy.maintainer`. */
    val maintainer: String get() = getProp(PROP_MAINTAINER).orEmpty()

    /** Device codename used to build the default OTA manifest URL (e.g. "a32"). */
    val deviceCodename: String
        get() = getProp("ro.product.device").orEmpty().ifBlank { Build.DEVICE ?: "unknown" }

    /** A-Only vs A/B, detected from the ROM slot suffix property. */
    val isAOnly: Boolean get() = getProp("ro.boot.slot_suffix").isNullOrEmpty()

    const val PROP_ROM_VER = "ro.cloudy.rom.ver"
    const val PROP_ROM_VER_CODE = "ro.cloudy.rom.ver.code"
    const val PROP_MAINTAINER = "ro.cloudy.maintainer"

    fun getProp(key: String): String? = runCatching {
        val p = Runtime.getRuntime().exec(arrayOf("getprop", key))
        p.inputStream.bufferedReader().readLine()?.trim()
    }.getOrNull()
}
