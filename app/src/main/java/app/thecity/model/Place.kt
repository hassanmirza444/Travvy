package app.thecity.model

import com.google.maps.android.clustering.ClusterItem
import com.google.android.gms.maps.model.LatLng
import java.io.Serializable
import java.util.ArrayList

class Place : Serializable, ClusterItem {
    var place_id = 0
    @JvmField
    var name = ""
    var image = ""
    @JvmField
    var address = ""
    var phone = ""
    var website = ""
    var description = ""
    var lng = 0.0
    var lat = 0.0
    var last_update: Long = 0
    @JvmField
    var distance = -1f
    var categories: List<Category> = ArrayList()
    var images: List<Images> = ArrayList()
    override fun getPosition(): LatLng {
        return LatLng(lat, lng)
    }

    val isDraft: Boolean
        get() = address == "" && phone == "" && website == "" && description == ""
}