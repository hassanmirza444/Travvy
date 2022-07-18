package app.thecity.fragment

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.thecity.ActivityMain
import app.thecity.ActivityPlaceDetail
import app.thecity.R
import app.thecity.adapter.AdapterPlaceGrid
import app.thecity.connection.RestAdapter
import app.thecity.connection.callbacks.CallbackListPlace
import app.thecity.data.*
import app.thecity.model.Place
import app.thecity.utils.Tools
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentCategory : Fragment(), AdapterPlaceGrid.OnItemClickListener,
    AdapterPlaceGrid.OnLoadMoreListener {
    private var count_total = 0
    private var category_id = 0
    private var root_view: View? = null
    private var recyclerView: RecyclerView? = null
    private var lyt_progress: View? = null
    private var lyt_not_found: View? = null
    private var text_progress: TextView? = null
    private var snackbar_retry: Snackbar? = null
    private var db: DatabaseHandler? = null
    private var sharedPref: SharedPref? = null
    private var adapter: AdapterPlaceGrid? = null
    private var callback: Call<CallbackListPlace>? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root_view = inflater.inflate(R.layout.fragment_category, null)

        // activate fragment menu
        setHasOptionsMenu(true)
        db = DatabaseHandler(requireActivity())
        sharedPref = SharedPref(requireActivity())
        category_id = requireArguments().getInt(TAG_CATEGORY)
        recyclerView = root_view!!.findViewById<View>(R.id.recycler) as RecyclerView
        lyt_progress = root_view!!.findViewById(R.id.lyt_progress)
        lyt_not_found = root_view!!.findViewById(R.id.lyt_not_found)
        text_progress = root_view!!.findViewById<View>(R.id.text_progress) as TextView
        recyclerView!!.layoutManager = StaggeredGridLayoutManager(
           /* Tools.getGridSpanCount(activity!!)*/2,
            StaggeredGridLayoutManager.VERTICAL
        )

        //set data and list adapter
        adapter = AdapterPlaceGrid(requireActivity(), recyclerView!!, ArrayList())
        recyclerView!!.adapter = adapter

        // on item list clicked
        adapter!!.setOnItemClickListener (this)
        recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(v: RecyclerView, state: Int) {
                super.onScrollStateChanged(v, state)
                if (state == RecyclerView.SCROLL_STATE_DRAGGING || state == RecyclerView.SCROLL_STATE_SETTLING) {
                    // ActivityMain.animateFab(true);
                } else {
                    //  ActivityMain.animateFab(false);
                }
            }
        })
        if (sharedPref!!.isRefreshPlaces || db!!.placesSize == 0) {
            actionRefresh(sharedPref!!.lastPlacePage)
        } else {
            startLoadMoreAdapter()
        }
        return root_view
    }

    override fun onDestroyView() {
        if (snackbar_retry != null) snackbar_retry!!.dismiss()
        if (callback != null && callback!!.isExecuted) {
            callback!!.cancel()
        }
        super.onDestroyView()
    }

    override fun onResume() {
        adapter!!.notifyDataSetChanged()
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_category, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_refresh) {
            ThisApplication.instance!!.location = null
            sharedPref!!.lastPlacePage = 1
            sharedPref!!.isRefreshPlaces = true
            text_progress!!.text = ""
            if (snackbar_retry != null) snackbar_retry!!.dismiss()
            actionRefresh(sharedPref!!.lastPlacePage)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startLoadMoreAdapter() {
        adapter!!.resetListData()
        val items = db!!.getPlacesByPage(category_id, Constant.LIMIT_LOADMORE, 0)
        adapter!!.insertData(items)
        showNoItemView()
        // detect when scroll reach bottom
        adapter!!.setOnLoadMoreListener (this)
    }

    private fun displayDataByPage(next_page: Int) {
        adapter!!.setLoading()
        Handler().postDelayed({
            val items = db!!.getPlacesByPage(
                category_id,
                Constant.LIMIT_LOADMORE,
                next_page * Constant.LIMIT_LOADMORE
            )
            adapter!!.insertData(items)
            showNoItemView()
        }, 500)
    }

    // checking some condition before perform refresh data
    private fun actionRefresh(page_no: Int) {
        val conn = Tools.cekConnection(activity)
        if (conn) {
            if (!onProcess) {
                onRefresh(page_no)
            } else {
                Snackbar.make(root_view!!, R.string.task_running, Snackbar.LENGTH_SHORT).show()
            }
        } else {
            onFailureRetry(page_no, getString(R.string.no_internet))
        }
    }

    private var onProcess = false
    private fun onRefresh(page_no: Int) {
        onProcess = true
        showProgress(onProcess)
        callback = RestAdapter.createAPI().getPlacesByPage(
            page_no,
            Constant.LIMIT_PLACE_REQUEST,
            if (AppConfig.LAZY_LOAD) 1 else 0
        )
        callback!!.enqueue(object : Callback<CallbackListPlace?> {
            override fun onResponse(
                call: Call<CallbackListPlace?>,
                response: Response<CallbackListPlace?>
            ) {
                val resp = response.body()
                if (resp != null) {
                    count_total = resp.count_total
                    if (page_no == 1) db!!.refreshTablePlace()
                    db!!.insertListPlaceAsync(resp.places) // save result into database
                    sharedPref!!.lastPlacePage = page_no + 1
                    delayNextRequest(page_no)
                    val str_progress = String.format(
                        getString(R.string.load_of),
                        page_no * Constant.LIMIT_PLACE_REQUEST,
                        count_total
                    )
                    text_progress!!.text = str_progress
                } else {
                    onFailureRetry(page_no, getString(R.string.refresh_failed))
                }
            }

            override fun onFailure(call: Call<CallbackListPlace?>, t: Throwable) {
                if (!call.isCanceled) {
                    Log.e("onFailure", t.message!!)
                    val conn = Tools.cekConnection(activity)
                    if (conn) {
                        onFailureRetry(page_no, getString(R.string.refresh_failed))
                    } else {
                        onFailureRetry(page_no, getString(R.string.no_internet))
                    }
                }
            }
        })
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            lyt_progress!!.visibility = View.VISIBLE
            recyclerView!!.visibility = View.GONE
            lyt_not_found!!.visibility = View.GONE
        } else {
            lyt_progress!!.visibility = View.GONE
            recyclerView!!.visibility = View.VISIBLE
        }
    }

    private fun showNoItemView() {
        if (adapter!!.itemCount == 0) {
            lyt_not_found!!.visibility = View.VISIBLE
        } else {
            lyt_not_found!!.visibility = View.GONE
        }
    }

    private fun onFailureRetry(page_no: Int, msg: String) {
        onProcess = false
        showProgress(onProcess)
        showNoItemView()
        startLoadMoreAdapter()
        try {
            snackbar_retry = Snackbar.make(root_view!!, msg, Snackbar.LENGTH_INDEFINITE)
            snackbar_retry!!.setAction(R.string.RETRY) { actionRefresh(page_no) }
            snackbar_retry!!.show()
        } catch (e: Exception) {
        }
    }

    private fun delayNextRequest(page_no: Int) {
        if (count_total == 0) {
            onFailureRetry(page_no, getString(R.string.refresh_failed))
            return
        }
        if (page_no * Constant.LIMIT_PLACE_REQUEST > count_total) { // when all data loaded
            onProcess = false
            showProgress(onProcess)
            startLoadMoreAdapter()
            sharedPref!!.isRefreshPlaces = false
            text_progress!!.text = ""
            Snackbar.make(root_view!!, R.string.load_success, Snackbar.LENGTH_LONG).show()
            return
        }
        Handler().postDelayed({ onRefresh(page_no + 1) }, 300)
    }

    companion object {
        var TAG_CATEGORY = "key.TAG_CATEGORY"
    }

    override fun onItemClick(view: View?, place: Place?) {
        ActivityPlaceDetail.navigate(
            activity as ActivityMain?, requireView().findViewById(R.id.lyt_content), place
        )
        try {
            (activity as ActivityMain?)!!.showInterstitialAd()
        } catch (e: Exception) {
        }
    }

    override fun onLoadMore(current_page: Int) {
        if (db!!.getPlacesSize(category_id) > adapter!!.itemCount && current_page != 0) {
            displayDataByPage(current_page)
        } else {
            adapter!!.setLoaded()
        }
    }
}