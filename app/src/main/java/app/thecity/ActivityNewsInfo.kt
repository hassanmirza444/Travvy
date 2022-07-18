package app.thecity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.thecity.adapter.AdapterNewsInfo
import app.thecity.connection.RestAdapter
import app.thecity.connection.callbacks.CallbackListNewsInfo
import app.thecity.data.Constant
import app.thecity.data.DatabaseHandler
import app.thecity.model.NewsInfo
import app.thecity.utils.Tools
import app.thecity.widget.SpacingItemDecoration
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ActivityNewsInfo : AppCompatActivity() ,AdapterNewsInfo.OnItemClickListener,
    AdapterNewsInfo.OnLoadMoreListener {
    var actionBar: ActionBar? = null
    private var parent_view: View? = null
    private var recyclerView: RecyclerView? = null
    private var mAdapter: AdapterNewsInfo? = null
    private var lyt_progress: View? = null
    private var callbackCall: Call<CallbackListNewsInfo>? = null
    private var db: DatabaseHandler? = null
    private var post_total = 0
    private var failed_page = 0
    private var snackbar_retry: Snackbar? = null

    // can be, ONLINE or OFFLINE
    private var MODE = "ONLINE"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_info)
        parent_view = findViewById(android.R.id.content)
        db = DatabaseHandler(this)
        initToolbar()
        iniComponent()
    }

    private fun initToolbar() {
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar!!.setHomeButtonEnabled(true)
        actionBar!!.setTitle(R.string.title_nav_news)
        Tools.systemBarLolipop(this)
    }

    fun iniComponent() {
        lyt_progress = findViewById(R.id.lyt_progress)
        recyclerView = findViewById<View>(R.id.recyclerView) as RecyclerView
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.addItemDecoration(SpacingItemDecoration(1, Tools.dpToPx(this, 4), true))


        //set data and list adapter
        mAdapter = AdapterNewsInfo(this, recyclerView!!, ArrayList())
        recyclerView!!.adapter = mAdapter

        mAdapter!!.setOnItemClickListener(this)
        mAdapter!!.setOnLoadMoreListener(this)
        // on item list clicked
       /* mAdapter!!.setOnItemClickListener { v, obj, position ->
            ActivityNewsInfoDetails.navigate(
                this@ActivityNewsInfo,
                obj,
                false
            )
        }*/

        // detect when scroll reach bottom
        /*mAdapter!!.setOnLoadMoreListener { current_page ->
            if (post_total > mAdapter!!.itemCount && current_page != 0) {
                val next_page = current_page.toString().toInt() + 1
                requestAction(next_page)
            } else {
                mAdapter!!.setLoaded()
            }
        }*/

        // if already have data news at db, use mode OFFLINE
        if (db!!.newsInfoSize > 0) {
            MODE = "OFFLINE"
        }
        requestAction(1)
    }

    private fun displayApiResult(items: List<NewsInfo>) {
        mAdapter!!.insertData(items)
        firstProgress(false)
        if (items.size == 0) {
            showNoItemView(true)
        }
    }

    private fun requestListNewsInfo(page_no: Int) {
        if (MODE == "ONLINE") {
            val api = RestAdapter.createAPI()
            callbackCall = api.getNewsInfoByPage(page_no, Constant.LIMIT_NEWS_REQUEST)
            callbackCall!!.enqueue(object : Callback<CallbackListNewsInfo?> {
                override fun onResponse(
                    call: Call<CallbackListNewsInfo?>,
                    response: Response<CallbackListNewsInfo?>
                ) {
                    val resp = response.body()
                    if (resp != null && resp.status == "success") {
                        if (page_no == 1) {
                            mAdapter!!.resetListData()
                            db!!.refreshTableNewsInfo()
                        }
                        post_total = resp.count_total
                        db!!.insertListNewsInfo(resp.news_infos)
                        displayApiResult(resp.news_infos)
                    } else {
                        onFailRequest(page_no)
                    }
                }

                override fun onFailure(call: Call<CallbackListNewsInfo?>, t: Throwable) {
                    if (!call.isCanceled) onFailRequest(page_no)
                }
            })
        } else {
            if (page_no == 1) mAdapter!!.resetListData()
            val limit = Constant.LIMIT_NEWS_REQUEST
            val offset = page_no * limit - limit
            post_total = db!!.newsInfoSize
            val items = db!!.getNewsInfoByPage(limit, offset)
            displayApiResult(items)
        }
    }

    private fun onFailRequest(page_no: Int) {
        failed_page = page_no
        mAdapter!!.setLoaded()
        firstProgress(false)
        if (Tools.cekConnection(this)) {
            showFailedView(true, getString(R.string.refresh_failed))
        } else {
            showFailedView(true, getString(R.string.no_internet))
        }
    }

    private fun requestAction(page_no: Int) {
        showFailedView(false, "")
        showNoItemView(false)
        if (page_no == 1) {
            firstProgress(true)
        } else {
            mAdapter!!.setLoading()
        }
        Handler().postDelayed(
            { requestListNewsInfo(page_no) },
            if (MODE == "OFFLINE") 50 else 1000.toLong()
        )
    }

    public override fun onDestroy() {
        super.onDestroy()
        firstProgress(false)
        if (callbackCall != null && callbackCall!!.isExecuted) {
            callbackCall!!.cancel()
        }
    }

    private fun showFailedView(show: Boolean, message: String) {
        if (snackbar_retry == null) {
            snackbar_retry = Snackbar.make(parent_view!!, "", Snackbar.LENGTH_INDEFINITE)
        }
        snackbar_retry!!.setText(message)
        snackbar_retry!!.setAction(R.string.RETRY) { requestAction(failed_page) }
        if (show) {
            snackbar_retry!!.show()
        } else {
            snackbar_retry!!.dismiss()
        }
    }

    private fun showNoItemView(show: Boolean) {
        val lyt_no_item = findViewById(R.id.lyt_no_item) as View
        if (show) {
            recyclerView!!.visibility = View.GONE
            lyt_no_item.visibility = View.VISIBLE
        } else {
            recyclerView!!.visibility = View.VISIBLE
            lyt_no_item.visibility = View.GONE
        }
    }

    private fun firstProgress(show: Boolean) {
        if (show) {
            lyt_progress!!.visibility = View.VISIBLE
        } else {
            lyt_progress!!.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activiy_news_info, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            super.onBackPressed()
        } else if (id == R.id.action_refresh) {
            if (callbackCall != null && callbackCall!!.isExecuted) callbackCall!!.cancel()
            showFailedView(false, "")
            MODE = "ONLINE"
            post_total = 0
            requestAction(1)
        } else if (id == R.id.action_settings) {
            val i = Intent(applicationContext, ActivitySetting::class.java)
            startActivity(i)
        } else if (id == R.id.action_rate) {
            Tools.rateAction(this@ActivityNewsInfo)
        } else if (id == R.id.action_about) {
            Tools.aboutAction(this@ActivityNewsInfo)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onResume() {
        if (actionBar != null) {
            Tools.setActionBarColor(this, actionBar!!)
            // for system bar in lollipop
            Tools.systemBarLolipop(this)
        }
        super.onResume()
    }

    override fun onItemClick(view: View?, obj: NewsInfo?, position: Int) {
        ActivityNewsInfoDetails.navigate(
            this@ActivityNewsInfo,
            obj!!,
            false
        )
    }

    override fun onLoadMore(current_page: Int) {
        if (post_total > mAdapter!!.itemCount && current_page != 0) {
            val next_page = current_page.toString().toInt() + 1
            requestAction(next_page)
        } else {
            mAdapter!!.setLoaded()
        }
    }
}