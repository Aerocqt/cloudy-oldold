package com.aerocat.cloudy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aerocat.cloudy.R
import com.aerocat.cloudy.ota.IFlashCallback
import com.aerocat.cloudy.ota.RootIpc
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
    private val rootIpc by lazy { RootIpc(requireContext().applicationContext) }
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
        b.rowInstalledVersion.summary = DeviceInfo.romVersion.ifBlank { "${DeviceInfo.PROP_ROM_VER} unset" }
        b.rowDeviceModel.summary = DeviceInfo.model
        b.rowAndroid.summary = DeviceInfo.androidVersion
        b.rowSecurity.summary = DeviceInfo.securityPatch
        b.rowFingerprint.summary = DeviceInfo.fingerprint
        b.rowKernel.summary = DeviceInfo.kernelVersion
    }

    private fun check() {
        b.downloadBar.isIndeterminate = true
        b.downloadBar.visibility = View.VISIBLE
        b.btnCheck.isEnabled = false
        lifecycleScope.launch {
            repo.fetchManifest(jsonUrl)
                .onSuccess { m ->
                    manifest = m
                    val r = m.release
                    b.rowBuildDate.summary = r.buildDate
                    b.changelog.text = r.changelog.joinToString("\n") { "•  $it" }
                    // Remote release rows (what you'd move TO).
                    b.rowRemoteVersion.summary = r.version
                    b.rowRemoteAndroid.summary = r.androidVersion
                    b.rowRemoteSecurity.summary = r.securityPatch
                    b.rowRemoteFingerprint.summary = r.fingerprint

                    val check = com.aerocat.cloudy.ota.VersionCheck.evaluate(r)
                    b.rowInstalledVersion.summary = check.installed
                    b.rowStatus.title = if (check.updateAvailable)
                        "Update available: ${r.version}" else "You are up to date"
                    b.btnDownload.isEnabled = check.updateAvailable
                }
                .onFailure { b.rowStatus.title = "Check failed: ${it.message}" }
            b.downloadBar.visibility = View.GONE
            b.btnCheck.isEnabled = true
        }
    }

    private fun downloadAndInstall(dl: Download) {
        val dest = File(requireContext().getExternalFilesDir(null), dl.filename)
        b.btnDownload.isEnabled = false
        lifecycleScope.launch {
            repo.download(dl, dest).collect { st ->
                when (st) {
                    is DownloadState.Progress -> {
                        b.downloadBar.isIndeterminate = false
                        b.downloadBar.visibility = View.VISIBLE
                        b.downloadBar.progress = (st.fraction * 100).toInt()
                        b.rowStatus.title = "Downloading ${(st.fraction * 100).toInt()}%"
                    }
                    is DownloadState.Failed -> {
                        b.rowStatus.title = st.reason
                        b.btnDownload.isEnabled = true
                    }
                    is DownloadState.Done -> install(st.file)
                }
            }
        }
    }

    private fun install(pkg: File) {
        val dl = manifest?.release?.download ?: return
        if (dl.installType.equals("raw_image", ignoreCase = true)) {
            confirmRawFlash(pkg, dl)   // dangerous path → explicit confirmation first
            return
        }
        // recovery_zip: safest tier first, then rooted recovery-staging fallback.
        val installer = OtaInstaller(requireContext())
        val result = when (val privileged = installer.tryPrivilegedInstall(pkg)) {
            is InstallResult.NeedsRoot -> installer.rootStageRecovery(pkg)
            else -> privileged
        }
        b.rowStatus.title = when (result) {
            is InstallResult.StagedRebootingToRecovery -> "Staged — rebooting to recovery to apply…"
            is InstallResult.NeedsRoot -> "Root + Cloudy module required (${result.why})"
            is InstallResult.Failed -> "Install failed: ${result.why}"
        }
    }

    /** Scary, unambiguous confirmation before any direct-to-partition write on an A-only device. */
    private fun confirmRawFlash(pkg: File, dl: Download) {
        val target = "system"   // for LumiROM full images; a boot/recovery image would pass its own name
        AlertDialog.Builder(requireContext())
            .setTitle("Flash directly to /$target?")
            .setMessage(
                "This writes the image straight to the $target partition.\n\n" +
                "The Galaxy A32 is A-only — there is no backup slot. If the write is " +
                "interrupted or the image is wrong, the device may not boot and will need " +
                "recovery/Odin to restore.\n\nOnly continue if you understand the risk."
            )
            .setPositiveButton("I understand, flash") { _, _ -> startRawFlash(pkg, target, dl.sizeBytes) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startRawFlash(pkg: File, partition: String, totalBytes: Long) {
        val progressView = layoutInflater.inflate(R.layout.dialog_flash_progress, null)
        val bar = progressView.findViewById<android.widget.ProgressBar>(R.id.flashBar)
        val label = progressView.findViewById<android.widget.TextView>(R.id.flashLabel)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Flashing $partition")
            .setView(progressView)
            .setCancelable(false)
            .create()
        dialog.show()

        lifecycleScope.launch {
            if (!rootIpc.connect()) {
                dialog.dismiss()
                b.rowStatus.title = "Root worker unavailable — is the Cloudy module installed?"
                return@launch
            }
            val cb = object : IFlashCallback.Stub() {
                override fun onProgress(percent: Int, line: String?) {
                    requireActivity().runOnUiThread {
                        if (percent >= 0) { bar.isIndeterminate = false; bar.progress = percent }
                        label.text = if (percent >= 0) "$percent%  ·  ${line.orEmpty()}" else line.orEmpty()
                    }
                }
                override fun onDone(success: Boolean, message: String?) {
                    requireActivity().runOnUiThread {
                        dialog.dismiss()
                        b.rowStatus.title = if (success)
                            "Flashed — rebooting to recovery to finalize…" else "Flash failed: ${message.orEmpty()}"
                        if (success) rootIpc.worker?.rebootRecovery()
                    }
                }
            }
            // Runs entirely in the root worker process; progress streams back via cb.
            rootIpc.worker?.rawFlash(pkg.absolutePath, partition, totalBytes, cb)
        }
    }

    override fun onDestroyView() {
        rootIpc.disconnect()
        super.onDestroyView(); _b = null
    }

    companion object {
        /**
         * Default manifest location. The device codename is auto-detected from
         * `ro.product.device` (falling back to Build.DEVICE), e.g. .../16.2/a32.json
         */
        private const val OTA_BASE = "https://raw.githubusercontent.com/cloudyota/ota-update/16.2"
        val DEFAULT_JSON_URL: String get() = "$OTA_BASE/${DeviceInfo.deviceCodename}.json"
    }
}
