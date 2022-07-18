package app.thecity.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.content.SharedPreferences
import android.widget.TextView
import android.widget.LinearLayout
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import app.thecity.R
import app.thecity.adapter.AdapterSuggestionSearch.SearchObject
import app.thecity.adapter.AdapterSuggestionSearch
import com.google.gson.Gson
import java.io.Serializable
import java.util.*

class AdapterSuggestionSearch(context: Context) :
    RecyclerView.Adapter<AdapterSuggestionSearch.ViewHolder>() {
    private var items: List<String?> = ArrayList()
    private var onItemClickListener: OnItemClickListener? = null
    private val prefs: SharedPreferences

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var title: TextView
        var lyt_parent: LinearLayout

        init {
            title = v.findViewById<View>(R.id.title) as TextView
            lyt_parent = v.findViewById<View>(R.id.lyt_parent) as LinearLayout
        }
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = items[position]
        holder.title.text = p
        holder.lyt_parent.setOnClickListener { v ->
            onItemClickListener!!.onItemClick(
                v, p,
                position
            )
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return items.size
    }

    interface OnItemClickListener {
        fun onItemClick(view: View?, viewModel: String?, pos: Int)
    }

    fun refreshItems() {
        items = searchHistory
        Collections.reverse(items)
        notifyDataSetChanged()
    }

    private inner class SearchObject(items: MutableList<String?>) : Serializable {
        var items: MutableList<String?> = ArrayList()

        init {
            this.items = items
        }
    }

    /**
     * To save last state request
     */
    fun addSearchHistory(s: String?) {
        val searchObject = SearchObject(searchHistory)
        if (searchObject.items.contains(s)) searchObject.items.remove(s)
        searchObject.items.add(s)
        if (searchObject.items.size > MAX_HISTORY_ITEMS) searchObject.items.removeAt(0)
        val json = Gson().toJson(searchObject, SearchObject::class.java)
        prefs.edit().putString(SEARCH_HISTORY_KEY, json).apply()
    }

    private val searchHistory: MutableList<String?>
        private get() {
            val json = prefs.getString(SEARCH_HISTORY_KEY, "")
            if (json == "") return ArrayList()
            val searchObject = Gson().fromJson(json, SearchObject::class.java)
            return searchObject.items
        }

    companion object {
        private const val SEARCH_HISTORY_KEY = "_SEARCH_HISTORY_KEY"
        private const val MAX_HISTORY_ITEMS = 5
    }

    init {
        prefs = context.getSharedPreferences("PREF_RECENT_SEARCH", Context.MODE_PRIVATE)
        items = searchHistory
        Collections.reverse(items)
    }
}