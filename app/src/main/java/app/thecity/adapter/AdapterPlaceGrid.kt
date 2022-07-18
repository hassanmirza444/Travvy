package app.thecity.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import app.thecity.model.Place
import android.widget.TextView
import android.widget.LinearLayout
import com.balysv.materialripple.MaterialRippleLayout
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import app.thecity.utils.Tools
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.thecity.R
import app.thecity.data.Constant
import java.util.ArrayList

class AdapterPlaceGrid(private val ctx: Context, view: RecyclerView, items: MutableList<Place?>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val VIEW_ITEM = 1
    private val VIEW_PROG = 0
    private var loading = false
    private var items: MutableList<Place?> = ArrayList()
    private var onLoadMoreListener: OnLoadMoreListener? = null
    private var onItemClickListener: OnItemClickListener? = null
    private var lastPosition = -1
    private var clicked = false

    interface OnItemClickListener {
        fun onItemClick(view: View?, viewModel: Place?)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        // each data item is just a string in this case
        var title: TextView
        var image: ImageView
        var distance: TextView
        var lyt_distance: LinearLayout
        var lyt_parent: MaterialRippleLayout

        init {
            title = v.findViewById<View>(R.id.title) as TextView
            image = v.findViewById<View>(R.id.image) as ImageView
            distance = v.findViewById<View>(R.id.distance) as TextView
            lyt_distance = v.findViewById<View>(R.id.lyt_distance) as LinearLayout
            lyt_parent = v.findViewById<View>(R.id.lyt_parent) as MaterialRippleLayout
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
        if (viewType == VIEW_ITEM) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
            vh = ViewHolder(v)
        } else {
            val v =
                LayoutInflater.from(parent.context).inflate(R.layout.item_loading, parent, false)
            vh = ProgressViewHolder(v)
        }
        return vh
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            val vItem = holder
            val p = items[position]
            vItem.title.text = p!!.name
            Tools.displayImageThumb(ctx, vItem.image, Constant.getURLimgPlace(p.image), 0.5f)
            if (p.distance == -1f) {
                vItem.lyt_distance.visibility = View.GONE
            } else {
                vItem.lyt_distance.visibility = View.VISIBLE
                vItem.distance.text = Tools.getFormatedDistance(p.distance)
            }

            // Here you apply the animation when the view is bound
            setAnimation(vItem.lyt_parent, position)
            vItem.lyt_parent.setOnClickListener { v ->
                if (!clicked && onItemClickListener != null) {
                    clicked = true
                    onItemClickListener!!.onItemClick(v, p)
                }
            }
            clicked = false
        } else {
            (holder as ProgressViewHolder).progressBar.isIndeterminate = true
        }
        if (getItemViewType(position) == VIEW_PROG) {
            val layoutParams =
                holder.itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams
            layoutParams.isFullSpan = true
        } else {
            val layoutParams =
                holder.itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams
            layoutParams.isFullSpan = false
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] != null) VIEW_ITEM else VIEW_PROG
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // Here is the key method to apply the animation
    private fun setAnimation(viewToAnimate: View, position: Int) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(
                ctx, R.anim.slide_in_bottom
            )
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    fun insertData(items: List<Place?>) {
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
        }
    }

    fun resetListData() {
        items = ArrayList()
        notifyDataSetChanged()
    }

    fun setOnLoadMoreListener(onLoadMoreListener: OnLoadMoreListener?) {
        this.onLoadMoreListener = onLoadMoreListener
    }

    private fun lastItemViewDetector(recyclerView: RecyclerView) {
        if (recyclerView.layoutManager is StaggeredGridLayoutManager) {
            val layoutManager = recyclerView.layoutManager as StaggeredGridLayoutManager?
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val lastPos =
                        getLastVisibleItem(layoutManager!!.findLastVisibleItemPositions(null))
                    if (!loading && lastPos == itemCount - 1 && onLoadMoreListener != null) {
                        val current_page = itemCount / Constant.LIMIT_LOADMORE
                        onLoadMoreListener!!.onLoadMore(current_page)
                        loading = true
                    }
                }
            })
        }
    }

    interface OnLoadMoreListener {
        fun onLoadMore(current_page: Int)
    }

    private fun getLastVisibleItem(into: IntArray): Int {
        var last_idx = into[0]
        for (i in into) {
            if (last_idx < i) last_idx = i
        }
        return last_idx
    }

    init {
        this.items = items
        lastItemViewDetector(view)
    }
}