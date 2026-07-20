package com.aerocat.cloudy.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.aerocat.cloudy.R
import com.aerocat.cloudy.databinding.ActivityMainBinding

/**
 * OneUI 8 shell:
 *   - ToolbarLayout gives the collapsing large title + subtitle (handled by the library).
 *   - BottomTabLayout is the OneUI bottom navigation bar, populated from menu_bottom_tabs.xml.
 * Fragments are swapped directly (no ViewPager2) which is the OneUI-native pattern.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val updateFragment by lazy { CheckUpdateFragment() }
    private val maintainerFragment by lazy { MaintainerFragment() }
    private val settingsFragment by lazy { SettingsFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomTab.inflateMenu(R.menu.menu_bottom_tabs) { item ->
            when (item.itemId) {
                R.id.tab_update -> show(updateFragment, R.string.tab_check_update)
                R.id.tab_maintainer -> show(maintainerFragment, R.string.tab_maintainer)
                R.id.tab_settings -> show(settingsFragment, R.string.tab_settings)
            }
            true
        }

        if (savedInstanceState == null) show(updateFragment, R.string.tab_check_update)
    }

    /** Swap the main_content fragment and update the collapsing header subtitle. */
    private fun show(fragment: Fragment, subtitleRes: Int) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(binding.fragmentContainer.id, fragment)
        }
        binding.toolbarLayout.setSubtitle(getString(subtitleRes))
    }
}
