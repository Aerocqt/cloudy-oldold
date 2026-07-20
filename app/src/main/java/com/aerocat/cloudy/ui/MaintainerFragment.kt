package com.aerocat.cloudy.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aerocat.cloudy.data.UpdateRepository
import com.aerocat.cloudy.databinding.FragmentMaintainerBinding
import kotlinx.coroutines.launch

/** Tab 2 — SESL card layout with maintainer profile, device and contact/credits actions. */
class MaintainerFragment : Fragment() {

    private var _b: FragmentMaintainerBinding? = null
    private val b get() = _b!!
    private val repo = UpdateRepository()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentMaintainerBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val url = requireContext().getSharedPreferences("cloudy", 0)
            .getString("json_url", CheckUpdateFragment.DEFAULT_JSON_URL)!!
        lifecycleScope.launch {
            repo.fetchManifest(url).onSuccess { m ->
                val mt = m.maintainer
                b.name.text = mt.name
                b.handle.text = mt.handle
                b.device.summary = "${mt.device} (${mt.codename})"
                b.rom.summary = m.romName
                b.btnTelegram.setOnClickListener { open(mt.telegram) }
                b.btnDonate.setOnClickListener { open(mt.donateUrl) }
            }
        }
    }

    private fun open(url: String?) {
        if (url.isNullOrBlank()) return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
