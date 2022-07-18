package app.thecity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import app.thecity.advertise.AdConfig
import app.thecity.advertise.AdNetworkHelper
import app.thecity.data.AppConfig
import app.thecity.data.DatabaseHandler
import app.thecity.data.SharedPref
import app.thecity.fragment.FragmentCategory
import app.thecity.utils.Tools
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class ActivityMain : AppCompatActivity() {
    var actionBar: ActionBar? = null
    var toolbar: Toolbar? = null
    private var cat: IntArray? = null
    private var fab: FloatingActionButton? = null
    private var navigationView: NavigationView? = null
    private var db: DatabaseHandler? = null
    private var sharedPref: SharedPref? = null
    private var nav_header_lyt: RelativeLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fab = findViewById<View>(R.id.fab) as FloatingActionButton
        db = DatabaseHandler(this)
        sharedPref = SharedPref(this)
        prepareAds()
        initToolbar()
        initDrawerMenu()
        cat = resources.getIntArray(R.array.id_category)

        // first drawer view
        onItemSelected(R.id.nav_all, getString(R.string.title_nav_all))
        fab!!.setOnClickListener {
            val i = Intent(this@ActivityMain, ActivitySearch::class.java)
            startActivity(i)
        }

        // for system bar in lollipop
        Tools.systemBarLolipop(this)
    }

    private fun initToolbar() {
        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar!!.setHomeButtonEnabled(true)
        Tools.setActionBarColor(this, actionBar!!)
    }

    private fun initDrawerMenu() {
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        val toggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this,
            drawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ) {
            override fun onDrawerOpened(drawerView: View) {
                updateFavoritesCounter(navigationView, R.id.nav_favorites, db!!.favoritesSize)
                super.onDrawerOpened(drawerView)
            }
        }
        drawer.setDrawerListener(toggle)
        toggle.syncState()
        navigationView = findViewById<View>(R.id.nav_view) as NavigationView
        navigationView!!.setNavigationItemSelectedListener { item ->
            showInterstitialAd()
            onItemSelected(item.itemId, item.title.toString())
        }
        if (!AppConfig.ENABLE_NEWS_INFO) navigationView!!.menu.removeItem(R.id.nav_news)

        // navigation header
        val nav_header = navigationView!!.getHeaderView(0)
        nav_header_lyt = nav_header.findViewById<View>(R.id.nav_header_lyt) as RelativeLayout
        nav_header_lyt!!.setBackgroundColor(Tools.colorBrighter(sharedPref!!.themeColorInt))
        nav_header.findViewById<View>(R.id.menu_nav_setting).setOnClickListener {
            val i = Intent(applicationContext, ActivitySetting::class.java)
            startActivity(i)
        }
        nav_header.findViewById<View>(R.id.menu_nav_map)
            .setOnClickListener {
                val i = Intent(applicationContext, ActivityMaps::class.java)
                startActivity(i)
            }
    }

    override fun onBackPressed() {
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        if (!drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START)
        } else {
            doExitApp()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_settings) {
            val i = Intent(applicationContext, ActivitySetting::class.java)
            startActivity(i)
        } else if (id == R.id.action_more) {
            Tools.directLinkToBrowser(this, getString(R.string.more_app_url))
        } else if (id == R.id.action_rate) {
            Tools.rateAction(this@ActivityMain)
        } else if (id == R.id.action_about) {
            Tools.aboutAction(this@ActivityMain)
        }
        return super.onOptionsItemSelected(item)
    }

    fun onItemSelected(id: Int, title: String?): Boolean {
        // Handle navigation view item clicks here.
        var fragment: Fragment? = null
        val bundle = Bundle()
        //sub menu
        /* IMPORTANT : cat[index_array], index is start from 0
         */if (id == R.id.nav_all) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, -1)
            actionBar!!.title = title
            // favorites
        } else if (id == R.id.nav_favorites) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, -2)
            actionBar!!.title = title
            // news info
        } else if (id == R.id.nav_news) {
            val i = Intent(this, ActivityNewsInfo::class.java)
            startActivity(i)
        } /*else if (id == R.id.nav_waterfalls) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(1) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_lakes) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(2) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_valleys) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(3) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_passes) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(4) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_peaks) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(5) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_forts) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(6) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_treks) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(7) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_dams) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(8) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_historical) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(9) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_ponds) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(10) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_hill_stations) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(11) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_glaciers) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(12) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_bridges) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(13) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_religious) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(14) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_beaches) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(15) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_tourist_spots) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(16) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_deserts) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(17) ?: 0)
            actionBar!!.title = title
        }*/

        else if (id == R.id.nav_waterfalls) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(0) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_lakes) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(1) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_valleys) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(2) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_passes) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(3) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_peaks) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(4) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_forts) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(5) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_treks) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(6) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_dams) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(7) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_historical) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(8) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_ponds) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(9) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_hill_stations) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(10) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_glaciers) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(11) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_bridges) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(12) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_religious) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(13) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_beaches) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(14) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_tourist_spots) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(15) ?: 0)
            actionBar!!.title = title
        } else if (id == R.id.nav_deserts) {
            fragment = FragmentCategory()
            bundle.putInt(FragmentCategory.TAG_CATEGORY, cat?.get(16) ?: 0)
            actionBar!!.title = title
        }


        if (fragment != null) {
            fragment.arguments = bundle
            val fragmentManager = supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.frame_content, fragment)
            fragmentTransaction.commit()
        }
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private var exitTime: Long = 0
    fun doExitApp() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            Toast.makeText(this, R.string.press_again_exit_app, Toast.LENGTH_SHORT).show()
            exitTime = System.currentTimeMillis()
        } else {
            finish()
        }
    }

    override fun onResume() {
        updateFavoritesCounter(navigationView, R.id.nav_favorites, db!!.favoritesSize)
        if (actionBar != null) {
            Tools.setActionBarColor(this, actionBar!!)
            // for system bar in lollipop
            Tools.systemBarLolipop(this)
        }
        if (nav_header_lyt != null) {
            nav_header_lyt!!.setBackgroundColor(Tools.colorBrighter(sharedPref!!.themeColorInt))
        }
        super.onResume()
    }

    public override fun onStart() {
        super.onStart()
        active = true
    }

    override fun onDestroy() {
        super.onDestroy()
        active = false
    }

    private fun updateFavoritesCounter(nav: NavigationView?, @IdRes itemId: Int, count: Int) {
        val view =
            nav!!.menu.findItem(itemId).actionView.findViewById<View>(R.id.counter) as TextView
        view.text = count.toString()
    }

    private var adNetworkHelper: AdNetworkHelper? = null
    private fun prepareAds() {
        adNetworkHelper = AdNetworkHelper(this)
        adNetworkHelper!!.showGDPR()
        adNetworkHelper!!.loadBannerAd(AdConfig.ADS_MAIN_BANNER)
        adNetworkHelper!!.loadInterstitialAd(AdConfig.ADS_MAIN_INTERSTITIAL)
    }

    fun showInterstitialAd() {
        adNetworkHelper!!.showInterstitialAd(AdConfig.ADS_MAIN_INTERSTITIAL)
    }

    companion object {
        @JvmField
        var active = false
    }
}