package com.aerocat.cloudy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aerocat.cloudy.data.Download
import com.aerocat.cloudy.data.DownloadState
import com.aerocat.cloudy.data.UpdateManifest
import com.aerocat.cloudy.data.UpdateRepository
import com.aerocat.cloudy.databinding.FragmentCheckUpdateBinding
import com.aerocat.cloudy.ota.DeviceInfo
import com.aerocat.cloudy.ota.InstallResult
import com.aerocat.cloudy.ota.OtaInstaller
import kotlinx.coroutines.launch
import java.io.File

class CheckUpdateFragment : Fragment() {

    private var _b: FragmentCheckUpdateBinding? = null
    private val b get() = _b!!
    private val repo = UpdateRepository()
    private var manifest: UpdateManifest? = null

    private val jsonUrl: String
        get() = requireContext()
            .getSharedPreferences("cloudy", 0)
            .getString("json_url", DEFAULT_JSON_URL)!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentCheckUpdateBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        renderLocalDeviceRows()
        b.btnCheck.setOnClickListener { check() }
        b.btnDownload.setOnClickListener { manifest?.let { downloadAndInstall(it.release.download) } }
        check()
    }

    private fun renderLocalDeviceRows() {
        b.rowDeviceModel.setSummary(DeviceInfo.model)
        b.rowAndroid.setSummary(DeviceInfo.androidVersion)
        b.rowSecurity.setSummary(DeviceInfo.securityPatch)
        b.rowFingerprint.setSummary(DeviceInfo.fingerprint)
        b.rowKernel.setSummary(DeviceInfo.kernelVersion)
    }

    private fun check() {
        b.progress.visibility = View.VISIBLE
        b.btnCheck.isEnabled = false
        lifecycleScope.launch {
            repo.fetchManifest(jsonUrl)
                .onSuccess { m ->
                    manifest = m
                    val r = m.release
                    b.rowBuildDate.setSummary(r.buildDate)
                    b.changelog.text = r.changelog.joinToString("\n") { "•  $it" }
                    // Remote release rows (what you'd move TO).
                    b.rowRemoteVersion.setSummary(r.version)
                    b.rowRemoteAndroid.setSummary(r.androidVersion)
                    b.rowRemoteSecurity.setSummary(r.securityPatch)
                    b.rowRemoteFingerprint.setSummary(r.fingerprint)

                    val newer = r.buildFingerprintDiffers()
                    b.status.text = if (newer) "Update available: ${r.version}" else "You are up to date"
                    b.btnDownload.isEnabled = newer
                }
                .onFailure { b.status.text = "Check failed: ${it.message}" }
            b.progress.visibility = View.GONE
            b.btnCheck.isEnabled = true
        }
    }

    private fun com.aerocat.cloudy.data.Release.buildFingerprintDiffers(): Boolean =
        fingerprint.isNotBlank() && fingerprint != DeviceInfo.fingerprint

    private fun downloadAndInstall(dl: Download) {
        val dest = File(requireContext().getExternalFilesDir(null), dl.filename)
        b.btnDownload.isEnabled = false
        lifecycleScope.launch {
            repo.download(dl, dest).collect { st ->
                when (st) {
                    is DownloadState.Progress -> {
                        b.downloadBar.progress = (st.fraction * 100).toInt()
                        b.status.text = "Downloading ${(st.fraction * 100).toInt()}%"
                    }
                    is DownloadState.Failed -> {
                        b.status.text = st.reason
                        b.btnDownload.isEnabled = true
                    }
                    is DownloadState.Done -> install(st.file)
                }
            }
        }
    }

    private fun install(pkg: File) {
        val installer = OtaInstaller(requireContext())
        // Safest tier first, then fall back to the rooted recovery-staging path.
        val result = when (val privileged = installer.tryPrivilegedInstall(pkg)) {
            is InstallResult.NeedsRoot -> installer.rootStageRecovery(pkg)
            else -> privileged
        }
        b.status.text = when (result) {
            is InstallResult.StagedRebootingToRecovery -> "Staged — rebooting to recovery to apply…"
            is InstallResult.NeedsRoot -> "Root + Cloudy module required (${result.why})"
            is InstallResult.Failed -> "Install failed: ${result.why}"
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }

    companion object {
        const val DEFAULT_JSON_URL = "https://raw.githubusercontent.com/LumiROM/ota/main/a32.json"
    }
}
