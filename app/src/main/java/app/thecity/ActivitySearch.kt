package app.thecity

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.thecity.adapter.AdapterPlaceGrid
import app.thecity.adapter.AdapterSuggestionSearch
import app.thecity.data.DatabaseHandler
import app.thecity.model.Place
import app.thecity.utils.Tools

class ActivitySearch : AppCompatActivity(), AdapterSuggestionSearch.OnItemClickListener,
    AdapterPlaceGrid.OnItemClickListener {
    private var toolbar: Toolbar? = null
    private var actionBar: ActionBar? = null
    private var et_search: EditText? = null
    private var bt_clear: ImageButton? = null
    private var parent_view: View? = null
    private var recyclerView: RecyclerView? = null
    private var mAdapter: AdapterPlaceGrid? = null
    private var recyclerSuggestion: RecyclerView? = null
    private var mAdapterSuggestion: AdapterSuggestionSearch? = null
    private var lyt_suggestion: LinearLayout? = null
    private var db: DatabaseHandler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        parent_view = findViewById(android.R.id.content)
        db = DatabaseHandler(this)
        initComponent()
        setupToolbar()
    }

    private fun initComponent() {
        lyt_suggestion = findViewById<View>(R.id.lyt_suggestion) as LinearLayout
        et_search = findViewById<View>(R.id.et_search) as EditText
        et_search!!.addTextChangedListener(textWatcher)
        bt_clear = findViewById<View>(R.id.bt_clear) as ImageButton
        bt_clear!!.visibility = View.GONE
        recyclerView = findViewById<View>(R.id.recyclerView) as RecyclerView
        recyclerSuggestion = findViewById<View>(R.id.recyclerSuggestion) as RecyclerView
        recyclerView!!.layoutManager = StaggeredGridLayoutManager(
            Tools.getGridSpanCount(this),
            StaggeredGridLayoutManager.VERTICAL
        )
        recyclerSuggestion!!.layoutManager = LinearLayoutManager(this)
        recyclerSuggestion!!.setHasFixedSize(true)

        //set data and list adapter
        mAdapter = AdapterPlaceGrid(this, recyclerView!!, ArrayList())
        recyclerView!!.adapter = mAdapter
        mAdapter!!.setOnItemClickListener(this)

        //set data and list adapter suggestion
        mAdapterSuggestion = AdapterSuggestionSearch(this)
        recyclerSuggestion!!.adapter = mAdapterSuggestion
        showSuggestionSearch()
        mAdapterSuggestion!!.setOnItemClickListener(this)
        bt_clear!!.setOnClickListener {
            et_search!!.setText("")
            mAdapter!!.resetListData()
            showNotFoundView()
        }
        et_search!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                searchAction()
                return@OnEditorActionListener true
            }
            false
        })
        et_search!!.setOnTouchListener { view, motionEvent ->
            showSuggestionSearch()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            false
        }
        showNotFoundView()
    }

    private fun showSuggestionSearch() {
        mAdapterSuggestion!!.refreshItems()
        lyt_suggestion!!.visibility = View.VISIBLE
    }

    override fun onResume() {
        mAdapter!!.notifyDataSetChanged()
        super.onResume()
    }

    private fun setupToolbar() {
        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        // for system bar in lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.grey_medium)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    var textWatcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(c: CharSequence, i: Int, i1: Int, i2: Int) {
            if (c.toString().trim { it <= ' ' }.length == 0) {
                bt_clear!!.visibility = View.GONE
            } else {
                bt_clear!!.visibility = View.VISIBLE
            }
        }

        override fun beforeTextChanged(c: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun afterTextChanged(editable: Editable) {}
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun searchAction() {
        lyt_suggestion!!.visibility = View.GONE
        showNotFoundView()
        val query = et_search!!.text.toString().trim { it <= ' ' }
        if (query != "") {
            mAdapterSuggestion!!.addSearchHistory(query)
            mAdapter!!.resetListData()
            mAdapter!!.insertData(Tools.filterItemsWithDistance(this,
                db!!.searchAllPlace(query) as List<Place>
            ))
            showNotFoundView()
        } else {
            Toast.makeText(this, R.string.please_fill, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNotFoundView() {
        val lyt_no_item = findViewById(R.id.lyt_no_item) as View
        if (mAdapter!!.itemCount == 0) {
            recyclerView!!.visibility = View.GONE
            lyt_no_item.visibility = View.VISIBLE
        } else {
            recyclerView!!.visibility = View.VISIBLE
            lyt_no_item.visibility = View.GONE
        }
    }

    override fun onItemClick(view: View?, viewModel: String?, pos: Int) {
        et_search!!.setText(viewModel)
        lyt_suggestion!!.visibility = View.GONE
        hideKeyboard()
        searchAction()
    }

    override fun onItemClick(view: View?, viewModel: Place?) {
        ActivityPlaceDetail.navigate(
            this@ActivitySearch, view!!.findViewById(
                R.id.lyt_content
            ), viewModel
        )
    }
}