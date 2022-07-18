package app.thecity

import androidx.appcompat.app.AppCompatActivity
import app.thecity.adapter.AdapterFullScreenImage
import androidx.viewpager.widget.ViewPager
import android.widget.TextView
import android.os.Bundle
import android.view.View
import app.thecity.ActivityFullScreenImage
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import android.widget.ImageButton
import app.thecity.utils.Tools
import java.util.ArrayList

class ActivityFullScreenImage : AppCompatActivity() {
    private var adapter: AdapterFullScreenImage? = null
    private var viewPager: ViewPager? = null
    private var text_page: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)
        viewPager = findViewById<View>(R.id.pager) as ViewPager
        text_page = findViewById<View>(R.id.text_page) as TextView
        var items = ArrayList<String>()
        val i = intent
        val position = i.getIntExtra(EXTRA_POS, 0)
        items = i.getStringArrayListExtra(EXTRA_IMGS) as ArrayList<String>
        adapter = AdapterFullScreenImage(this@ActivityFullScreenImage, items)
        val total = adapter!!.count
        viewPager!!.adapter = adapter
        text_page!!.text = String.format(getString(R.string.image_of), position + 1, total)

        // displaying selected image first
        viewPager!!.currentItem = position
        viewPager!!.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                pos: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(pos: Int) {
                text_page!!.text = String.format(getString(R.string.image_of), pos + 1, total)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        (findViewById<View>(R.id.btnClose) as ImageButton).setOnClickListener { finish() }

        // for system bar in lollipop
        Tools.systemBarLolipop(this)
    }

    override fun onResume() {
        super.onResume()
    }

    companion object {
        const val EXTRA_POS = "key.EXTRA_POS"
        const val EXTRA_IMGS = "key.EXTRA_IMGS"
    }
}