package app.thecity.adapter

import android.app.Activity
import android.content.Context
import androidx.viewpager.widget.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.view.ViewGroup
import com.github.chrisbanes.photoview.PhotoView
import app.thecity.utils.Tools
import androidx.viewpager.widget.ViewPager
import app.thecity.R

class AdapterFullScreenImage     // constructor
    (private val act: Activity, private val imagePaths: List<String>) : PagerAdapter() {
    private var inflater: LayoutInflater? = null
    override fun getCount(): Int {
        return imagePaths.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as RelativeLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val image: PhotoView
        inflater = act.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val viewLayout = inflater!!.inflate(R.layout.item_fullscreen_image, container, false)
        image = viewLayout.findViewById<View>(R.id.image) as PhotoView
        Tools.displayImage(act, image, imagePaths[position])
        (container as ViewPager).addView(viewLayout)
        return viewLayout
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        (container as ViewPager).removeView(`object` as RelativeLayout)
    }
}