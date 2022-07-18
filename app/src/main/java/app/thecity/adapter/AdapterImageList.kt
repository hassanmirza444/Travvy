package app.thecity.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import app.thecity.R
import app.thecity.data.Constant
import app.thecity.model.Images
import app.thecity.utils.Tools
import com.balysv.materialripple.MaterialRippleLayout

class AdapterImageList(private val ctx: Context, items: List<Images>) :
    RecyclerView.Adapter<AdapterImageList.ViewHolder>() {
    private var items: List<Images> = ArrayList()
    private var onItemClickListener: OnItemClickListener? = null
    private val lastPosition = -1

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        // each data item is just a string in this case
        var image: ImageView
        var lyt_parent: MaterialRippleLayout

        init {
            image = v.findViewById<View>(R.id.image) as ImageView
            lyt_parent = v.findViewById<View>(R.id.lyt_parent) as MaterialRippleLayout
        }
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ViewHolder(v)
    }

    // ReString the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = items[position].name
        Tools.displayImage(ctx, holder.image, Constant.getURLimgPlace(p))
        holder.lyt_parent.setOnClickListener { v -> // Give some delay to the ripple to finish the effect
            onItemClickListener!!.onItemClick(v, p, position)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return items.size
    }

    interface OnItemClickListener {
        fun onItemClick(view: View?, viewModel: String?, pos: Int)
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    init {
        this.items = items
    }
}