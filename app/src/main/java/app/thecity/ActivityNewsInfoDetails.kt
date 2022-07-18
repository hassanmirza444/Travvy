package app.thecity

import androidx.appcompat.app.AppCompatActivity
import app.thecity.model.NewsInfo
import android.os.Bundle
import app.thecity.ActivityNewsInfoDetails
import app.thecity.data.ThisApplication
import app.thecity.utils.Tools
import com.google.android.material.appbar.AppBarLayout
import app.thecity.data.SharedPref
import app.thecity.advertise.AdNetworkHelper
import app.thecity.advertise.AdConfig
import android.widget.TextView
import android.text.Html
import android.webkit.WebChromeClient
import android.os.Build
import android.view.View.OnTouchListener
import android.view.MotionEvent
import com.balysv.materialripple.MaterialRippleLayout
import app.thecity.ActivityFullScreenImage
import app.thecity.ActivityMain
import app.thecity.ActivitySplash
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import app.thecity.data.Constant
import java.util.ArrayList

class ActivityNewsInfoDetails : AppCompatActivity() {
    private var from_notif: Boolean? = null

    // extra obj
    private var newsInfo: NewsInfo? = null
    private var toolbar: Toolbar? = null
    private var actionBar: ActionBar? = null
    private var parent_view: View? = null
    private var webview: WebView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_info_details)
        newsInfo = intent.getSerializableExtra(EXTRA_OBJECT) as NewsInfo?
        from_notif = intent.getBooleanExtra(EXTRA_FROM_NOTIF, false)
        initComponent()
        initToolbar()
        displayData()
        prepareAds()

        // analytics tracking
        ThisApplication.instance!!.trackScreenView("View News Info : " + newsInfo!!.title)
    }

    private fun initComponent() {
        parent_view = findViewById(android.R.id.content)
    }

    private fun initToolbar() {
        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar!!.setHomeButtonEnabled(true)
        actionBar!!.title = ""
        Tools.systemBarLolipop(this)
        Tools.setActionBarColor(this, actionBar!!)
        (findViewById<View>(R.id.appbar) as AppBarLayout).setBackgroundColor(SharedPref(this).themeColorInt)
    }

    private fun prepareAds() {
        val adNetworkHelper = AdNetworkHelper(this)
        adNetworkHelper.loadBannerAd(AdConfig.ADS_NEWS_DETAILS_BANNER)
    }

    private fun displayData() {
        (findViewById<View>(R.id.title) as TextView).text = Html.fromHtml(
            newsInfo!!.title
        )
        webview = findViewById<View>(R.id.content) as WebView
        var html_data: String? =
            "<style>img{max-width:100%;height:auto;} iframe{width:100%;}</style> "
        html_data += newsInfo!!.full_content
        webview!!.settings.javaScriptEnabled = true
        webview!!.settings
        webview!!.settings.builtInZoomControls = true
        webview!!.setBackgroundColor(Color.TRANSPARENT)
        webview!!.webChromeClient = WebChromeClient()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            webview!!.loadDataWithBaseURL(
                null,
                html_data!!,
                "text/html; charset=UTF-8",
                "utf-8",
                null
            )
        } else {
            webview!!.loadData(html_data!!, "text/html; charset=UTF-8", null)
        }
        // disable scroll on touch
        webview!!.setOnTouchListener { v, event -> event.action == MotionEvent.ACTION_MOVE }
        (findViewById<View>(R.id.date) as TextView).text = Tools.getFormattedDate(
            newsInfo!!.last_update
        )
        Tools.displayImage(
            this, findViewById<View>(R.id.image) as ImageView, newsInfo!!.image?.let {
                Constant.getURLimgNews(
                    it
                )
            }
        )
        (findViewById<View>(R.id.lyt_image) as MaterialRippleLayout).setOnClickListener {
            val images_list = ArrayList<String>()
            newsInfo!!.image?.let { it1 -> Constant.getURLimgNews(it1) }
                ?.let { it2 -> images_list.add(it2) }
            val i = Intent(this@ActivityNewsInfoDetails, ActivityFullScreenImage::class.java)
            i.putStringArrayListExtra(ActivityFullScreenImage.EXTRA_IMGS, images_list)
            startActivity(i)
        }
    }

    override fun onPause() {
        super.onPause()
        if (webview != null) webview!!.onPause()
    }

    override fun onResume() {
        if (webview != null) webview!!.onResume()
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackAction()
            return true
        } else if (id == R.id.action_share) {
            Tools.methodShareNews(this, newsInfo!!)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        onBackAction()
    }

    private fun onBackAction() {
        if (from_notif!!) {
            if (ActivityMain.active) {
                finish()
            } else {
                val intent = Intent(applicationContext, ActivitySplash::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val EXTRA_OBJECT = "key.EXTRA_OBJECT"
        private const val EXTRA_FROM_NOTIF = "key.EXTRA_FROM_NOTIF"

        // activity transition
        fun navigate(activity: Activity, obj: NewsInfo, from_notif: Boolean?) {
            val i = navigateBase(activity, obj as NewsInfo, from_notif)
            activity.startActivity(i)
        }

        @JvmStatic
        fun navigateBase(context: Context?, obj: NewsInfo?, from_notif: Boolean?): Intent {
            val i = Intent(context, ActivityNewsInfoDetails::class.java)
            i.putExtra(EXTRA_OBJECT, obj as NewsInfo)
            i.putExtra(EXTRA_FROM_NOTIF, from_notif)
            return i
        }
    }
}