package app.thecity.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.thecity.R
import app.thecity.data.Constant
import app.thecity.model.NewsInfo
import app.thecity.utils.Tools

class AdapterNewsInfo(context: Context, view: RecyclerView, items: MutableList<NewsInfo?>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val VIEW_ITEM = 1
    private val VIEW_PROG = 0
    private var items: MutableList<NewsInfo?> = ArrayList()
    private var loading = false
    private val ctx: Context
    private var onLoadMoreListener: OnLoadMoreListener? = null
    private var mOnItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(view: View?, obj: NewsInfo?, position: Int)
    }

    fun setOnItemClickListener(mItemClickListener: OnItemClickListener) {
        mOnItemClickListener = mItemClickListener
    }

    inner class OriginalViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        // each data item is just a string in this case
        var title: TextView
        var brief_content: TextView
        var image: ImageView
        var lyt_parent: LinearLayout

        init {
            title = v.findViewById<View>(R.id.title) as TextView
            brief_content = v.findViewById<View>(R.id.brief_content) as TextView
            image = v.findViewById<View>(R.id.image) as ImageView
            lyt_parent = v.findViewById<View>(R.id.lyt_parent) as LinearLayout
        }
    }

    class ProgressViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var progressBar: ProgressBar

        init {
            progressBar = v.findViewById<View>(R.id.progressBar1) as ProgressBar
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val vh: RecyclerView.ViewHolder
        vh = if (viewType == VIEW_ITEM) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_news_info, parent, false)
            OriginalViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_loading, parent, false)
            ProgressViewHolder(v)
        }
        return vh
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is OriginalViewHolder) {
            val o = items[position]
            val vItem = holder
            vItem.title.text = o!!.title
            vItem.brief_content.text = o.brief_content
            Tools.displayImageThumb(ctx, vItem.image, Constant.getURLimgNews(o.image!!), 0.5f)
            vItem.lyt_parent.setOnClickListener { view ->
                if (mOnItemClickListener != null) {
                    mOnItemClickListener!!.onItemClick(view, o, position)
                }
            }
        } else {
            (holder as ProgressViewHolder).progressBar.isIndeterminate = true
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] != null) VIEW_ITEM else VIEW_PROG
    }

    fun insertData(items: List<NewsInfo?>) {
        setLoaded()
        val positionStart = itemCount
        val itemCount = items.size
        this.items.addAll(items)
        notifyItemRangeInserted(positionStart, itemCount)
    }

    fun setLoaded() {
        loading = false
        for (i in 0 until itemCount) {
            if (items[i] == null) {
                items.removeAt(i)
                notifyItemRemoved(i)
            }
        }
    }

    fun setLoading() {
        if (itemCount != 0) {
            items.add(null)
            notifyItemInserted(itemCount - 1)
            loading = true
        }
    }

    fun resetListData() {
        items = ArrayList()
        notifyDataSetChanged()
    }

    fun setOnLoadMoreListener(onLoadMoreListener: OnLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener
    }

    private fun lastItemViewDetector(recyclerView: RecyclerView) {
        if (recyclerView.layoutManager is LinearLayoutManager) {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val lastPos = layoutManager!!.findLastVisibleItemPosition()
                    if (!loading && lastPos == itemCount - 1 && onLoadMoreListener != null) {
                        if (onLoadMoreListener != null) {
                            val current_page = itemCount / Constant.LIMIT_NEWS_REQUEST
                            onLoadMoreListener!!.onLoadMore(current_page)
                        }
                        loading = true
                    }
                }
            })
        }
    }

    interface OnLoadMoreListener {
        fun onLoadMore(current_page: Int)
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    init {
        this.items = items
        ctx = context
        lastItemViewDetector(view)
    }
}