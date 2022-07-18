package app.thecity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.thecity.adapter.AdapterImageList
import app.thecity.advertise.AdConfig
import app.thecity.advertise.AdNetworkHelper
import app.thecity.connection.RestAdapter
import app.thecity.connection.callbacks.CallbackPlaceDetails
import app.thecity.data.Constant
import app.thecity.data.DatabaseHandler
import app.thecity.data.SharedPref
import app.thecity.data.ThisApplication
import app.thecity.model.Images
import app.thecity.model.Place
import app.thecity.utils.Tools
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ActivityPlaceDetail : AppCompatActivity() , AdapterImageList.OnItemClickListener {
    private var place: Place? = null
    private var fab: FloatingActionButton? = null
    private var description: WebView? = null
    private var parent_view: View? = null
    private var googleMap: GoogleMap? = null
    private var db: DatabaseHandler? = null
    private var onProcess = false
    private var isFromNotif = false
    private var callback: Call<CallbackPlaceDetails>? = null
    private var lyt_progress: View? = null
    private var lyt_distance: View? = null
    private var distance = -1f
    private var snackbar: Snackbar? = null
    private var new_images_str: ArrayList<String>? = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_details)
        parent_view = findViewById(android.R.id.content)
        db = DatabaseHandler(this)
        // animation transition
        ViewCompat.setTransitionName(findViewById(R.id.app_bar_layout), EXTRA_OBJ)
        place = intent.getSerializableExtra(EXTRA_OBJ) as Place?
        isFromNotif = intent.getBooleanExtra(EXTRA_NOTIF_FLAG, false)
        fab = findViewById<View>(R.id.fab) as FloatingActionButton
        lyt_progress = findViewById(R.id.lyt_progress)
        lyt_distance = findViewById(R.id.lyt_distance)
        if (place!!.image != null) {
            Tools.displayImage(
                this, findViewById<View>(R.id.image) as ImageView, Constant.getURLimgPlace(
                    place!!.image
                )
            )
        }
        fabToggle()
        setupToolbar(if (place!!.name == null) "" else place!!.name)
        initMap()
        prepareAds()

        // handle when favorite button clicked
        fab!!.setOnClickListener {
            if (db!!.isFavoritesExist(place!!.place_id)) {
                db!!.deleteFavorites(place!!.place_id)
                Snackbar.make(
                    parent_view!!,
                    place!!.name + " " + getString(R.string.remove_favorite),
                    Snackbar.LENGTH_SHORT
                ).show()
                // analytics tracking
                //ThisApplication.instance.trackEvent(Constant.Event.FAVORITES.name, "REMOVE", place!!.name)
            } else {
                db!!.addFavorites(place!!.place_id)
                Snackbar.make(
                    parent_view!!,
                    place!!.name + " " + getString(R.string.add_favorite),
                    Snackbar.LENGTH_SHORT
                ).show()
                // analytics tracking
                ThisApplication.instance!!.trackEvent(Constant.Event.FAVORITES.name, "ADD", place!!.name)
            }
            fabToggle()
        }

        // for system bar in lollipop
        Tools.systemBarLolipop(this)

        // analytics tracking
       // ThisApplication.instance.trackScreenView("View place : " + if (place!!.name == null) "name" else place!!.name)
    }

    private fun displayData(p: Place?) {
        setupToolbar(place!!.name)
        Tools.displayImage(
            this, findViewById<View>(R.id.image) as ImageView, Constant.getURLimgPlace(
                place!!.image
            )
        )
        (findViewById<View>(R.id.address) as TextView).text = p!!.address
        (findViewById<View>(R.id.phone) as TextView).text =
            if (p.phone == "-" || p.phone.trim { it <= ' ' } == "") getString(
                R.string.no_phone_number
            ) else p.phone
        (findViewById<View>(R.id.website) as TextView).text =
            if (p.website == "-" || p.website.trim { it <= ' ' } == "") getString(
                R.string.no_website
            ) else p.website
        description = findViewById<View>(R.id.description) as WebView
        var html_data: String? =
            "<style>img{max-width:100%;height:auto;} iframe{width:100%;}</style> "
        html_data += p.description
        description!!.settings.builtInZoomControls = true
        description!!.setBackgroundColor(Color.TRANSPARENT)
        description!!.webChromeClient = WebChromeClient()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            description!!.loadDataWithBaseURL(
                null,
                html_data!!,
                "text/html; charset=UTF-8",
                "utf-8",
                null
            )
        } else {
            description!!.loadData(html_data!!, "text/html; charset=UTF-8", null)
        }
        description!!.settings.javaScriptEnabled = true
        // disable scroll on touch
        description!!.setOnTouchListener { v, event -> event.action == MotionEvent.ACTION_MOVE }
        distance = place!!.distance
        if (distance == -1f) {
            lyt_distance!!.visibility = View.GONE
        } else {
            lyt_distance!!.visibility = View.VISIBLE
            (findViewById<View>(R.id.distance) as TextView).text =
                Tools.getFormatedDistance(distance)
        }
        setImageGallery(db!!.getListImageByPlaceId(p.place_id))
    }

    override fun onResume() {
        loadPlaceData()
        if (description != null) description!!.onResume()
        super.onResume()
    }

    // this method name same with android:onClick="clickLayout" at layout xml
    fun clickLayout(view: View) {
        val id = view.id
        if (id == R.id.lyt_address) {
            if (!place!!.isDraft) {
                val uri =
                    Uri.parse("http://maps.google.com/maps?q=loc:" + place!!.lat + "," + place!!.lng)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        } else if (id == R.id.lyt_phone) {
            if (!place!!.isDraft && place!!.phone != "-" && place!!.phone.trim { it <= ' ' } != "") {
                Tools.dialNumber(this, place!!.phone)
            } else {
                Snackbar.make(parent_view!!, R.string.fail_dial_number, Snackbar.LENGTH_SHORT)
                    .show()
            }
        } else if (id == R.id.lyt_website) {
            if (!place!!.isDraft && place!!.website != "-" && place!!.website.trim { it <= ' ' } != "") {
                Tools.directUrl(this, place!!.website)
            } else {
                Snackbar.make(parent_view!!, R.string.fail_open_website, Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun setImageGallery(images: List<Images>) {
        // add optional image into list
        val new_images: MutableList<Images> = ArrayList()
        new_images.add(Images(place!!.place_id, place!!.image))
        new_images.addAll(images)
        new_images_str = ArrayList()
        for (img in new_images) {
            new_images_str!!.add(Constant.getURLimgPlace(img.name))
        }
        val galleryRecycler = findViewById<View>(R.id.galleryRecycler) as RecyclerView
        galleryRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val adapter = AdapterImageList(this, new_images)
        galleryRecycler.adapter = adapter
        adapter.setOnItemClickListener(this)
    }

    private fun openImageGallery(position: Int) {
        val i = Intent(this@ActivityPlaceDetail, ActivityFullScreenImage::class.java)
        i.putExtra(ActivityFullScreenImage.EXTRA_POS, position)
        i.putStringArrayListExtra(ActivityFullScreenImage.EXTRA_IMGS, new_images_str)
        startActivity(i)
    }

    private fun fabToggle() {
        if (db!!.isFavoritesExist(place!!.place_id)) {
            fab!!.setImageResource(R.drawable.ic_nav_favorites)
        } else {
            fab!!.setImageResource(R.drawable.ic_nav_favorites_outline)
        }
    }

    private fun setupToolbar(name: String) {
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar.title = ""
        (findViewById<View>(R.id.toolbar_title) as TextView).text = name
        val collapsing_toolbar =
            findViewById<View>(R.id.collapsing_toolbar) as CollapsingToolbarLayout
        collapsing_toolbar.setContentScrimColor(SharedPref(this).themeColorInt)
        (findViewById<View>(R.id.app_bar_layout) as AppBarLayout).addOnOffsetChangedListener(
            OnOffsetChangedListener { appBarLayout, verticalOffset ->
                if (collapsing_toolbar.height + verticalOffset < 2 * ViewCompat.getMinimumHeight(
                        collapsing_toolbar
                    )
                ) {
                    fab!!.show()
                } else {
                    fab!!.hide()
                }
            })
        findViewById<View>(R.id.image).setOnClickListener(View.OnClickListener {
            if (new_images_str == null || new_images_str!!.size <= 0) return@OnClickListener
            openImageGallery(0)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            backAction()
            return true
        } else if (id == R.id.action_share) {
            if (!place!!.isDraft) {
                Tools.methodShare(this@ActivityPlaceDetail, place!!)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initMap() {
        if (googleMap == null) {
            val mapFragment1 = fragmentManager.findFragmentById(R.id.mapPlaces) as MapFragment
            mapFragment1.getMapAsync { gMap ->
                googleMap = gMap
                if (googleMap == null) {
                    Snackbar.make(parent_view!!, R.string.unable_create_map, Snackbar.LENGTH_SHORT)
                        .show()
                } else {
                    // config map
                    googleMap = Tools.configStaticMap(this@ActivityPlaceDetail, googleMap!!, place!!)
                }
            }
        }
        (findViewById<View>(R.id.bt_navigate) as Button).setOnClickListener { //Toast.makeText(getApplicationContext(),"OPEN", Toast.LENGTH_LONG).show();
            val navigation = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?daddr=" + place!!.lat + "," + place!!.lng)
            )
            startActivity(navigation)
        }
        (findViewById<View>(R.id.bt_view) as Button).setOnClickListener { openPlaceInMap() }
        (findViewById<View>(R.id.map) as LinearLayout).setOnClickListener { openPlaceInMap() }
    }

    private fun openPlaceInMap() {
        val intent = Intent(this@ActivityPlaceDetail, ActivityMaps::class.java)
        intent.putExtra(ActivityMaps.EXTRA_OBJ, place)
        startActivity(intent)
    }

    private fun prepareAds() {
        val adNetworkHelper = AdNetworkHelper(this)
        adNetworkHelper.loadBannerAd(AdConfig.ADS_PLACE_DETAILS_BANNER)
    }

    override fun onDestroy() {
        if (callback != null && callback!!.isExecuted) callback!!.cancel()
        super.onDestroy()
    }

    override fun onBackPressed() {
        backAction()
    }

    override fun onPause() {
        super.onPause()
        if (description != null) description!!.onPause()
    }

    private fun backAction() {
        if (isFromNotif) {
            val i = Intent(this, ActivityMain::class.java)
            startActivity(i)
        }
        finish()
    }

    // places detail load with lazy scheme
    private fun loadPlaceData() {
        place = db!!.getPlace(place!!.place_id)
        if (place!!.isDraft) {
            if (Tools.cekConnection(this)) {
                requestDetailsPlace(place!!.place_id)
            } else {
                onFailureRetry(getString(R.string.no_internet))
            }
        } else {
            displayData(place)
        }
    }

    private fun requestDetailsPlace(place_id: Int) {
        if (onProcess) {
            Snackbar.make(parent_view!!, R.string.task_running, Snackbar.LENGTH_SHORT).show()
            return
        }
        onProcess = true
        showProgressbar(true)
        callback = RestAdapter.createAPI().getPlaceDetails(place_id)
        callback!!.enqueue(object : Callback<CallbackPlaceDetails?> {
            override fun onResponse(
                call: Call<CallbackPlaceDetails?>,
                response: Response<CallbackPlaceDetails?>
            ) {
                val resp = response.body()
                if (resp != null) {
                    place = db!!.updatePlace(resp.place)
                    displayDataWithDelay(place)
                } else {
                    onFailureRetry(getString(R.string.failed_load_details))
                }
            }

            override fun onFailure(call: Call<CallbackPlaceDetails?>, t: Throwable) {
                if (call != null && !call.isCanceled) {
                    val conn = Tools.cekConnection(this@ActivityPlaceDetail)
                    if (conn) {
                        onFailureRetry(getString(R.string.failed_load_details))
                    } else {
                        onFailureRetry(getString(R.string.no_internet))
                    }
                }
            }
        })
    }

    private fun displayDataWithDelay(resp: Place?) {
        Handler().postDelayed({
            showProgressbar(false)
            onProcess = false
            displayData(resp)
        }, 1000)
    }

    private fun onFailureRetry(msg: String) {
        showProgressbar(false)
        onProcess = false
        snackbar = Snackbar.make(parent_view!!, msg, Snackbar.LENGTH_INDEFINITE)
        snackbar!!.setAction(R.string.RETRY) { loadPlaceData() }
        snackbar!!.show()
        retryDisplaySnackbar()
    }

    private fun retryDisplaySnackbar() {
        if (snackbar != null && !snackbar!!.isShown) {
            Handler().postDelayed({ retryDisplaySnackbar() }, 1000)
        }
    }

    private fun showProgressbar(show: Boolean) {
        lyt_progress!!.visibility = if (show) View.VISIBLE else View.GONE
    }

    companion object {
        private const val EXTRA_OBJ = "key.EXTRA_OBJ"
        private const val EXTRA_NOTIF_FLAG = "key.EXTRA_NOTIF_FLAG"

        // give preparation animation activity transition
        fun navigate(activity: AppCompatActivity?, sharedView: View?, p: Place?) {
            val intent = Intent(activity, ActivityPlaceDetail::class.java)
            intent.putExtra(EXTRA_OBJ, p)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity!!, sharedView!!, EXTRA_OBJ
            )
            ActivityCompat.startActivity(activity, intent, options.toBundle())
        }

        @JvmStatic
        fun navigateBase(context: Context?, obj: Place?, from_notif: Boolean?): Intent {
            val i = Intent(context, ActivityPlaceDetail::class.java)
            i.putExtra(EXTRA_OBJ, obj)
            i.putExtra(EXTRA_NOTIF_FLAG, from_notif)
            return i
        }
    }

    override fun onItemClick(view: View?, viewModel: String?, pos: Int) {
        openImageGallery(pos as Int)
    }
}