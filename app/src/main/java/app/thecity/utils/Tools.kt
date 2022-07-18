package app.thecity.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import app.thecity.R
import app.thecity.data.AppConfig
import app.thecity.data.SharedPref
import app.thecity.data.ThisApplication
import app.thecity.model.DeviceInfo
import app.thecity.model.NewsInfo
import app.thecity.model.Place
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.net.URI
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object Tools {
    @JvmStatic
    fun needRequestPermission(): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1
    }

    val isLolipopOrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    fun systemBarLolipop(act: Activity) {
        if (isLolipopOrHigher) {
            val window = act.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = colorDarker(SharedPref(act).themeColorInt)
        }
    }

    fun cekConnection(context: Context?): Boolean {
        val conn = ConnectionDetector(context)
        return if (conn.isConnectingToInternet) {
            true
        } else {
            false
        }
    }

    fun displayImage(ctx: Context, img: ImageView?, url: String?) {
        try {
            Glide.with(ctx.applicationContext).load(url)
                .transition(DrawableTransitionOptions.withCrossFade())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(img!!)
        } catch (e: Exception) {
        }
    }

    fun displayImageThumb(ctx: Context, img: ImageView?, url: String?, thumb: Float) {
        try {
            Glide.with(ctx.applicationContext).load(url)
                .transition(DrawableTransitionOptions.withCrossFade())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .thumbnail(thumb)
                .into(img!!)
        } catch (e: Exception) {
        }
    }

    fun clearImageCacheOnBackground(ctx: Context?) {
        try {
            Thread { Glide.get(ctx!!).clearDiskCache() }.start()
        } catch (e: Exception) {
        }
    }

    val deviceName: String
        get() {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.startsWith(manufacturer)) {
                model
            } else {
                "$manufacturer $model"
            }
        }
    val androidVersion: String
        get() = Build.VERSION.RELEASE + ""

    fun getGridSpanCount(activity: Activity): Int {
        val display = activity.windowManager.defaultDisplay
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val cellWidth = activity.resources.getDimension(R.dimen.item_place_width)
        return Math.round(screenWidth / cellWidth)
    }

    fun configStaticMap(act: Activity, googleMap: GoogleMap, place: Place): GoogleMap {
        // set map type
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        // Enable / Disable zooming controls
        googleMap.uiSettings.isZoomControlsEnabled = false
        // Enable / Disable my location button
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        // Enable / Disable Compass icon
        googleMap.uiSettings.isCompassEnabled = false
        // Enable / Disable Rotate gesture
        googleMap.uiSettings.isRotateGesturesEnabled = false
        // Enable / Disable zooming functionality
        googleMap.uiSettings.isZoomGesturesEnabled = false
        // enable traffic layer
        googleMap.isTrafficEnabled
        googleMap.isTrafficEnabled = false
        googleMap.uiSettings.isScrollGesturesEnabled = false
        googleMap.uiSettings.isMapToolbarEnabled = false
        val inflater = act.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val marker_view = inflater.inflate(R.layout.maps_marker, null)
        (marker_view.findViewById<View>(R.id.marker_bg) as ImageView).setColorFilter(
            act.resources.getColor(
                R.color.marker_secondary
            )
        )
        val cameraPosition = CameraPosition.Builder().target(place.position).zoom(12f).build()
        val markerOptions = MarkerOptions().position(place.position)
        markerOptions.icon(
            BitmapDescriptorFactory.fromBitmap(
                createBitmapFromView(
                    act,
                    marker_view
                )
            )
        )
        googleMap.addMarker(markerOptions)
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        return googleMap
    }

    fun configActivityMaps(googleMap: GoogleMap): GoogleMap {
        // set map type
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        // Enable / Disable zooming controls
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Enable / Disable Compass icon
        googleMap.uiSettings.isCompassEnabled = true
        // Enable / Disable Rotate gesture
        googleMap.uiSettings.isRotateGesturesEnabled = true
        // Enable / Disable zooming functionality
        googleMap.uiSettings.isZoomGesturesEnabled = true
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = true
        return googleMap
    }

    fun rateAction(activity: Activity) {
        val uri = Uri.parse("market://details?id=" + activity.packageName)
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            activity.startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + activity.packageName)
                )
            )
        }
    }

    private fun getPlayStoreUrl(act: Activity): String {
        return "http://play.google.com/store/apps/details?id=" + act.packageName
    }

    fun aboutAction(activity: Activity) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(activity.getString(R.string.dialog_about_title))
        builder.setMessage(activity.getString(R.string.about_text))
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    fun dialNumber(ctx: Context, phone: String) {
        try {
            val i = Intent(Intent.ACTION_DIAL)
            i.data = Uri.parse("tel:$phone")
            ctx.startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Cannot dial number", Toast.LENGTH_SHORT)
        }
    }

    fun directUrl(ctx: Context, website: String) {
        var url = website
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            url = "http://$url"
        }
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        ctx.startActivity(i)
    }

    fun methodShare(act: Activity, p: Place) {

        // string to share
        val shareBody = """
               View good place '${p.name}'
               located at : ${p.address}
               
               Using app : ${getPlayStoreUrl(act)}
               """.trimIndent()
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, act.getString(R.string.app_name))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        act.startActivity(Intent.createChooser(sharingIntent, "Share Using"))
    }

    fun methodShareNews(act: Activity, n: NewsInfo) {

        // string to share
        val shareBody = """
               ${n.title}
               
               ${getPlayStoreUrl(act)}
               """.trimIndent()
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, act.getString(R.string.app_name))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        act.startActivity(Intent.createChooser(sharingIntent, "Share Using"))
    }

    fun createBitmapFromView(act: Activity, view: View): Bitmap {
        val displayMetrics = DisplayMetrics()
        act.windowManager.defaultDisplay.getMetrics(displayMetrics)
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels)
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        view.buildDrawingCache()
        val bitmap =
            Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    fun setActionBarColor(ctx: Context?, actionbar: ActionBar) {
        val colordrw = ColorDrawable(SharedPref(ctx!!).themeColorInt)
        actionbar.setBackgroundDrawable(colordrw)
    }

    fun colorDarker(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.8f // value component
        return Color.HSVToColor(hsv)
    }

    fun colorBrighter(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] /= 0.8f // value component
        return Color.HSVToColor(hsv)
    }

    private fun calculateDistance(from: LatLng, to: LatLng): Float {
        val start = Location("")
        start.latitude = from.latitude
        start.longitude = from.longitude
        val end = Location("")
        end.latitude = to.latitude
        end.longitude = to.longitude
        val distInMeters = start.distanceTo(end)
        var resultDist = 0f
        resultDist = if (AppConfig.DISTANCE_METRIC_CODE == "KILOMETER") {
            distInMeters / 1000
        } else {
            (distInMeters * 0.000621371192).toFloat()
        }
        return resultDist
    }

    fun filterItemsWithDistance(act: Activity, items: List<Place>): List<Place> {
        if (AppConfig.SORT_BY_DISTANCE) { // checking for distance sorting
            val curLoc = getCurLocation(act)
            if (curLoc != null) {
                return getSortedDistanceList(items, curLoc)
            }
        }
        return items
    }

    fun itemsWithDistance(ctx: Context, items: List<Place>): List<Place> {
        if (AppConfig.SORT_BY_DISTANCE) { // checking for distance sorting
            val curLoc = getCurLocation(ctx)
            if (curLoc != null) {
                return getDistanceList(items, curLoc)
            }
        }
        return items
    }

    fun getDistanceList(places: List<Place>, curLoc: LatLng): List<Place> {
        if (places.size > 0) {
            for (p in places) {
                p.distance = calculateDistance(curLoc, p.position)
            }
        }
        return places
    }

    fun getSortedDistanceList(places: List<Place>, curLoc: LatLng): List<Place> {
        val result: MutableList<Place> = ArrayList()
        if (places.size > 0) {
            for (i in places.indices) {
                val p = places[i]
                p.distance = calculateDistance(curLoc, p.position)
                result.add(p)
            }
            Collections.sort(result) { p1, p2 -> java.lang.Float.compare(p1.distance, p2.distance) }
        } else {
            return places
        }
        return result
    }

    fun getCurLocation(ctx: Context): LatLng? {
        if (PermissionUtil.isLocationGranted(ctx)) {
            val manager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                var loc = ThisApplication.instance!!.location
                if (loc == null) {
                    loc = getLastKnownLocation(ctx)
                    ThisApplication.instance!!.location = loc
                }
                if (loc != null) {
                    return LatLng(loc.latitude, loc.longitude)
                }
            }
        }
        return null
    }

    fun getLastKnownLocation(ctx: Context): Location? {
        // add location listener
        setLocationListener(ctx)
        val mLocationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationListener = requestLocationUpdate(mLocationManager)
        val providers = mLocationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val l = mLocationManager.getLastKnownLocation(provider!!) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                // Found best last known location: %s", l);
                bestLocation = l
            }
        }
        mLocationManager.removeUpdates(locationListener)
        return bestLocation
    }

    private fun requestLocationUpdate(manager: LocationManager): LocationListener {
        // Define a listener that responds to location updates
        val locationListener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {}
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        // Register the listener with the Location Manager to receive location updates
        for (provider in manager.allProviders) {
            manager.requestLocationUpdates(provider, 0, 0f, locationListener)
        }
        return locationListener
    }

    private var locationCallback: LocationCallback? = null
    private var locationProviderClient: FusedLocationProviderClient? = null
    private fun setLocationListener(ctx: Context) {
        if (locationCallback != null) return
        if (locationProviderClient == null) {
            locationProviderClient = LocationServices.getFusedLocationProviderClient(ctx)
        }
        // for getting the current location update after every 2 seconds with high accuracy
        val locationRequest = LocationRequest()
            .setInterval(500).setFastestInterval(500)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationProviderClient!!.removeLocationUpdates(locationCallback!!)
                if (locationResult.locations == null) return
                for (location in locationResult.locations) {
                    if (location != null) {
                        ThisApplication.instance!!.setLocation(location)
                    }
                }
            }
        }
        locationProviderClient!!.requestLocationUpdates(locationRequest,
            locationCallback as LocationCallback, Looper.getMainLooper()
        )
    }

    fun getFormatedDistance(distance: Float): String {
        val df = DecimalFormat()
        df.maximumFractionDigits = 1
        return df.format(distance.toDouble()) + " " + AppConfig.DISTANCE_METRIC_STR
    }

    fun getDeviceInfo(context: Context): DeviceInfo {
        var phoneID = Build.SERIAL
        try {
            phoneID = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
        }
        val deviceInfo = DeviceInfo()
        deviceInfo.device = deviceName
        deviceInfo.email = phoneID
        deviceInfo.version = androidVersion
        deviceInfo.regid = SharedPref(context).fcmRegId
        deviceInfo.date_create = System.currentTimeMillis()
        return deviceInfo
    }

    fun getFormattedDateSimple(dateTime: Long?): String {
        val newFormat = SimpleDateFormat("MMM dd, yyyy")
        return newFormat.format(Date(dateTime!!))
    }

    fun getFormattedDate(dateTime: Long?): String {
        val newFormat = SimpleDateFormat("MMMM dd, yyyy hh:mm")
        return newFormat.format(Date(dateTime!!))
    }

    fun dpToPx(c: Context, dp: Int): Int {
        val r = c.resources
        return Math.round(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                r.displayMetrics
            )
        )
    }

    fun directLinkToBrowser(activity: Activity, url: String?) {
        var url = url
        url = appendQuery(url, "t=" + System.currentTimeMillis())
        if (!URLUtil.isValidUrl(url)) {
            Toast.makeText(activity, "Ops, Cannot open url", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(intent)
    }

    private fun appendQuery(uri: String?, appendQuery: String): String? {
        return try {
            val oldUri = URI(uri)
            var newQuery = oldUri.query
            if (newQuery == null) {
                newQuery = appendQuery
            } else {
                newQuery += "&$appendQuery"
            }
            val newUri = URI(
                oldUri.scheme,
                oldUri.authority,
                oldUri.path, newQuery, oldUri.fragment
            )
            newUri.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            uri
        }
    }

    interface CallbackRegId {
        fun onSuccess(result: DeviceInfo?)
        fun onError()
    }
}