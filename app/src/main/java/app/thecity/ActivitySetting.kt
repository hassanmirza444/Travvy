package app.thecity

import androidx.appcompat.app.AppCompatDelegate
import app.thecity.data.SharedPref
import android.os.Bundle
import app.thecity.ActivitySetting
import app.thecity.data.AppConfig
import android.preference.Preference.OnPreferenceClickListener
import android.content.DialogInterface
import app.thecity.utils.Tools
import com.google.android.material.snackbar.Snackbar
import android.preference.Preference.OnPreferenceChangeListener
import app.thecity.data.ThisApplication
import android.app.Activity
import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Color
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.AdapterView
import androidx.annotation.LayoutRes
import android.text.TextUtils
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.preference.*
import android.view.*
import android.widget.ListView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import app.thecity.data.Constant

/**
 * ATTENTION : To see where list of setting comes is open res/xml/setting_notification.xml
 */
class ActivitySetting : PreferenceActivity() {
    private var mDelegate: AppCompatDelegate? = null
    private var actionBar: ActionBar? = null
    private var parent_view: View? = null
    private var sharedPref: SharedPref? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.setting_notification)
        parent_view = findViewById(android.R.id.content) as View
        sharedPref = SharedPref(applicationContext)
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_ringtone)))
        val notifPref = findPreference(getString(R.string.pref_key_notif)) as Preference
        val resetCachePref = findPreference(getString(R.string.pref_key_reset_cache)) as Preference
        val themePref = findPreference(getString(R.string.pref_key_theme)) as Preference
        val ratePref = findPreference("key_rate") as Preference
        val morePref = findPreference("key_more") as Preference
        val aboutPref = findPreference("key_about") as Preference
        val prefTerm = findPreference(getString(R.string.pref_title_term)) as Preference
        if (!AppConfig.THEME_COLOR) {
            val categoryOthers =
                findPreference(getString(R.string.pref_category_display)) as PreferenceCategory
            categoryOthers.removePreference(themePref)
        }
        resetCachePref.onPreferenceClickListener = OnPreferenceClickListener {
            val builder = AlertDialog.Builder(this@ActivitySetting)
            builder.setTitle(getString(R.string.dialog_confirm_title))
            builder.setMessage(getString(R.string.message_clear_image_cache))
            builder.setPositiveButton("OK") { dialogInterface, i ->
                Tools.clearImageCacheOnBackground(this@ActivitySetting)
                Snackbar.make(
                    parent_view!!,
                    getString(R.string.message_after_clear_image_cache),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            builder.setNegativeButton(R.string.CANCEL, null)
            builder.show()
            true
        }
        notifPref.onPreferenceChangeListener = OnPreferenceChangeListener { preference, o ->
            val flag = o as Boolean
            // analytics tracking
            ThisApplication.instance!!.trackEvent(
                Constant.Event.NOTIFICATION.name,
                if (flag) "ENABLE" else "DISABLE",
                "-"
            )
            true
        }
        ratePref.onPreferenceClickListener = OnPreferenceClickListener {
            Tools.rateAction(this@ActivitySetting)
            true
        }
        morePref.onPreferenceClickListener = OnPreferenceClickListener {
            Tools.directLinkToBrowser(this@ActivitySetting, getString(R.string.more_app_url))
            true
        }
        aboutPref.onPreferenceClickListener = OnPreferenceClickListener {
            Tools.aboutAction(this@ActivitySetting)
            true
        }
        themePref.onPreferenceClickListener = OnPreferenceClickListener {
            dialogColorChooser(this@ActivitySetting)
            // analytics tracking
            ThisApplication.instance!!.trackEvent(Constant.Event.THEME.name, "CHANGE", "-")
            true
        }
        prefTerm.onPreferenceClickListener = OnPreferenceClickListener {
            dialogTerm(this@ActivitySetting)
            true
        }
    }

    override fun onResume() {
        initToolbar()
        super.onResume()
    }

    fun dialogTerm(activity: Activity) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(activity.getString(R.string.pref_title_term))
        builder.setMessage(activity.getString(R.string.content_term))
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    private fun dialogColorChooser(activity: Activity) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE) // before
        dialog.setContentView(R.layout.dialog_color_theme)
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window!!.attributes)
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        val list = dialog.findViewById<View>(R.id.list_view) as ListView
        val stringArray = resources.getStringArray(R.array.arr_main_color_name)
        val colorCode = resources.getStringArray(R.array.arr_main_color_code)
        list.adapter = object : ArrayAdapter<String?>(
            this@ActivitySetting,
            android.R.layout.simple_list_item_1,
            stringArray
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getView(position, convertView, parent) as TextView
                textView.width = ViewGroup.LayoutParams.MATCH_PARENT
                textView.height = ViewGroup.LayoutParams.MATCH_PARENT
                textView.setBackgroundColor(Color.parseColor(colorCode[position]))
                textView.setTextColor(Color.WHITE)
                return textView
            }
        }
        list.onItemClickListener = AdapterView.OnItemClickListener { av, v, pos, id ->
            sharedPref!!.themeColor = colorCode[pos]
            dialog.dismiss()
            onResume()
        }

        //global.setIntPref(global.I_PREF_COLOR, global.I_KEY_COLOR, getResources().getColor(R.color.red));
        dialog.show()
        dialog.window!!.attributes = lp
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delegate.onPostCreate(savedInstanceState)
    }

    private fun initToolbar() {
        actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar!!.setHomeButtonEnabled(true)
        actionBar!!.setTitle(R.string.activity_title_settings)
        // for system bar in lollipop
        Tools.systemBarLolipop(this)
        Tools.setActionBarColor(this, actionBar!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    /*
     * Support for Activity : DO NOT CODE BELOW ----------------------------------------------------
     */
    val supportActionBar: ActionBar?
        get() = delegate.supportActionBar

    fun setSupportActionBar(toolbar: Toolbar?) {
        delegate.setSupportActionBar(toolbar)
    }

    override fun getMenuInflater(): MenuInflater {
        return delegate.menuInflater
    }

    override fun setContentView(@LayoutRes layoutResID: Int) {
        delegate.setContentView(layoutResID)
    }

    override fun setContentView(view: View) {
        delegate.setContentView(view)
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams) {
        delegate.setContentView(view, params)
    }

    override fun addContentView(view: View, params: ViewGroup.LayoutParams) {
        delegate.addContentView(view, params)
    }

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()
    }

    override fun onTitleChanged(title: CharSequence, color: Int) {
        super.onTitleChanged(title, color)
        delegate.setTitle(title)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        delegate.onConfigurationChanged(newConfig)
    }

    override fun onStop() {
        super.onStop()
        delegate.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        delegate.onDestroy()
    }

    override fun invalidateOptionsMenu() {
        delegate.invalidateOptionsMenu()
    }

    private val delegate: AppCompatDelegate
        private get() {
            if (mDelegate == null) {
                mDelegate = AppCompatDelegate.create(this, null)
            }
            return mDelegate as AppCompatDelegate
        }

    companion object {
        /**
         * Binds a preference's summary to its value. More specifically, when the preference's value is changed.
         */
        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                PreferenceManager.getDefaultSharedPreferences(preference.context)
                    .getString(preference.key, "")
            )
        }

        /**
         * A preference value change listener that updates the preference's summary to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener =
            OnPreferenceChangeListener { preference, value ->
                val stringValue = value.toString()
                if (preference is ListPreference) {
                    // For list preferences, look up the correct display value in the preference's 'entries' list.
                    val listPreference = preference
                    val index = listPreference.findIndexOfValue(stringValue)

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                        if (index >= 0) listPreference.entries[index] else null
                    )
                } else if (preference is RingtonePreference) {
                    // For ringtone preferences, look up the correct display value using RingtoneManager.
                    if (TextUtils.isEmpty(stringValue)) {
                        // Empty values correspond to 'silent' (no ringtone).
                        preference.setSummary(R.string.pref_ringtone_silent)
                    } else {
                        val ringtone = RingtoneManager.getRingtone(
                            preference.getContext(),
                            Uri.parse(stringValue)
                        )
                        if (ringtone == null) {
                            // Clear the summary if there was a lookup error.
                            preference.setSummary(null)
                        } else {
                            // Set the summary to reflect the new ringtone display name.
                            val name = ringtone.getTitle(preference.getContext())
                            preference.setSummary(name)
                        }
                    }
                } else {
                    // For all other preferences, set the summary to the value's simple string representation.
                    preference.summary = stringValue
                }
                true
            }
    }
}