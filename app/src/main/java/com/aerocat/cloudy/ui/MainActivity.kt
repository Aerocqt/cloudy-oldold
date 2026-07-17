package com.aerocat.cloudy.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aerocat.cloudy.R
import com.aerocat.cloudy.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator

/**
 * OneUI shell: a collapsing ToolbarLayout header + a SESL TabLayout wired to a ViewPager2.
 * Tab 0 = Check Update, Tab 1 = Maintainer. The settings gear routes to SettingsFragment.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Large bold title that alpha-fades between expanded and collapsed states (OneUI behaviour).
        binding.toolbarLayout.setTitle(getString(R.string.app_name))
        binding.toolbarLayout.setExpandedSubtitle(getString(R.string.settings_entry_summary))

        binding.viewPager.adapter = TabsAdapter(this)
        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, pos ->
            tab.text = when (pos) {
                0 -> getString(R.string.tab_check_update)
                else -> getString(R.string.tab_maintainer)
            }
        }.attach()

        binding.toolbarLayout.setNavigationButtonOnClickListener {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .add(android.R.id.content, SettingsFragment())
                .addToBackStack("settings")
                .commit()
        }
    }

    private class TabsAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount() = 2
        override fun createFragment(position: Int): Fragment =
            if (position == 0) CheckUpdateFragment() else MaintainerFragment()
    }
}
