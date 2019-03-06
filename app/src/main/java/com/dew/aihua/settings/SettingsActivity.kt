package com.dew.aihua.settings

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.dew.aihua.R
import com.dew.aihua.player.helper.ThemeHelper
import com.dew.aihua.settings.preference_fragment.MainSettingsFragment


/**
 *  Created by Edward on 3/2/2019.
 */
class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceBundle: Bundle?) {
        setTheme(ThemeHelper.getSettingsThemeStyle(this))

        super.onCreate(savedInstanceBundle)
        setContentView(R.layout.settings_layout)

        // don't use kotlin-extensions here because toolbar layout is shared.
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (savedInstanceBundle == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_holder, MainSettingsFragment())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()

        with(window){
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = ContextCompat.getColor(themedContext, R.color.dark_aihua_dark_color)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val actionBar = supportActionBar
        actionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(true)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            if (supportFragmentManager.backStackEntryCount == 0) {
                finish()
            } else
                supportFragmentManager.popBackStack()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, preference: Preference): Boolean {
        @Suppress("DEPRECATION")
        val fragment = androidx.fragment.app.Fragment.instantiate(this, preference.fragment, preference.extras)
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.animator.custom_fade_in, R.animator.custom_fade_out, R.animator.custom_fade_in, R.animator.custom_fade_out)
            .replace(R.id.fragment_holder, fragment)
            .addToBackStack(null)
            .commit()
        return true
    }

    companion object {

        fun initSettings(context: Context) {
            AppSettings.initSettings(context)
        }
    }
}
