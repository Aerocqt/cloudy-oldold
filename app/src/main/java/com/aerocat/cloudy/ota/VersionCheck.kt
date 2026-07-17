package com.aerocat.cloudy.ota

import com.aerocat.cloudy.data.Release

/**
 * Decides whether a remote [Release] is newer than what's installed, using the ROM's own
 * stamp `ro.cloudy.version` (and `ro.cloudy.version_code` when available) as the source of truth.
 *
 * Order of preference:
 *   1. Integer compare: manifest.version_code  vs  ro.cloudy.version_code
 *   2. Semver compare:  manifest.version        vs  ro.cloudy.version
 *   3. Fallback:        build fingerprint differs (last resort, least reliable)
 */
object VersionCheck {

    data class Result(val updateAvailable: Boolean, val installed: String, val reason: String)

    fun evaluate(release: Release): Result {
        val installedVersion = DeviceInfo.cloudyVersion                     // ro.cloudy.version
        val installedCode = DeviceInfo.getProp("ro.cloudy.version_code")?.toLongOrNull()

        // 1) numeric version codes — cleanest
        if (release.versionCode != null && installedCode != null) {
            return Result(
                release.versionCode > installedCode,
                installedVersion.ifBlank { installedCode.toString() },
                "version_code $installedCode → ${release.versionCode}"
            )
        }

        // 2) semver from ro.cloudy.version
        if (installedVersion.isNotBlank()) {
            val cmp = compareSemver(extractSemver(release.version), extractSemver(installedVersion))
            return Result(cmp > 0, installedVersion, "ro.cloudy.version $installedVersion")
        }

        // 3) fingerprint fallback (non-LumiROM base, prop unset)
        val differs = release.fingerprint.isNotBlank() &&
                release.fingerprint != DeviceInfo.fingerprint
        return Result(differs, "unknown (ro.cloudy.version unset)", "fingerprint fallback")
    }

    /** Pulls the first x[.y[.z]] token out of a label like "LumiROM 8.6.4 Beta". */
    private fun extractSemver(s: String): List<Int> =
        Regex("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?").find(s)
            ?.let { m -> m.groupValues.drop(1).map { it.toIntOrNull() ?: 0 } }
            ?: listOf(0, 0, 0)

    private fun compareSemver(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val d = (a.getOrElse(i) { 0 }) - (b.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }
}
