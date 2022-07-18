package app.thecity.data

import android.util.Log
import java.util.logging.Logger

object Constant {
    /**
     * -------------------- EDIT THIS WITH YOURS ---------------------------------------------------
     */
    // Edit WEB_URL with your url. Make sure you have backslash('/') in the end url
    // public static String WEB_URL = "http://demo.dream-space.web.id/the_city/";
    @JvmField
    var WEB_URL = "http://travelbuddy.risenexpress.com/"

    // for map zoom
    const val city_lat = -6.9174639
    const val city_lng = 107.6191228

    /**
     * ------------------- DON'T EDIT THIS ---------------------------------------------------------
     */
    // image file url
    @JvmStatic
    fun getURLimgPlace(file_name: String): String {
        Log.d("hssn",WEB_URL + "uploads/place/" + file_name)
         return WEB_URL + "uploads/place/" + file_name;
       // return "https://d3b4d950pk995t.cloudfront.net/$file_name"
    }

    @JvmStatic
    fun getURLimgNews(file_name: String): String {
        return WEB_URL + "uploads/news/" + file_name
    }

    // this limit value used for give pagination (request and display) to decrease payload
    const val LIMIT_PLACE_REQUEST = 40
    const val LIMIT_LOADMORE = 40
    const val LIMIT_NEWS_REQUEST = 40

    // retry load image notification
    var LOAD_IMAGE_NOTIF_RETRY = 3

    // for search logs Tag
    const val LOG_TAG = "CITY_LOG"

    // Google analytics event category
    enum class Event {
        FAVORITES, THEME, NOTIFICATION, REFRESH
    }
}