package app.thecity.fcm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import app.thecity.ActivityNewsInfoDetails.Companion.navigateBase
import app.thecity.ActivityPlaceDetail.Companion.navigateBase
import app.thecity.ActivitySplash
import app.thecity.R
import app.thecity.data.AppConfig
import app.thecity.data.Constant.getURLimgNews
import app.thecity.data.Constant.getURLimgPlace
import app.thecity.data.DatabaseHandler
import app.thecity.data.SharedPref
import app.thecity.model.FcmNotif
import app.thecity.model.NewsInfo
import app.thecity.model.Place
import app.thecity.utils.PermissionUtil
import app.thecity.utils.Tools
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson

class FcmMessagingService : FirebaseMessagingService() {
    private var sharedPref: SharedPref? = null
    override fun onNewToken(s: String) {
        super.onNewToken(s)
        sharedPref = SharedPref(this)
        sharedPref!!.fcmRegId = s
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        sharedPref = SharedPref(this)
        sharedPref!!.isRefreshPlaces = true
        if (AppConfig.REFRESH_IMG_NOTIF) {
            Tools.clearImageCacheOnBackground(this)
        }
        if (sharedPref!!.notification && PermissionUtil.isStorageGranted(this)) {
            val fcmNotif = FcmNotif()
            if (remoteMessage.data.size > 0) {
                val data = remoteMessage.data
                fcmNotif.title = data["title"]
                fcmNotif.content = data["content"]
                fcmNotif.type = data["type"]

                // load data place if exist
                val place_str = data["place"]
                fcmNotif.place =
                    if (place_str != null) Gson().fromJson(place_str, Place::class.java) else null

                // load data news_info if exist
                val news_str = data["news"]
                fcmNotif.news =
                    if (news_str != null) Gson().fromJson(news_str, NewsInfo::class.java) else null
            } else if (remoteMessage.notification != null) {
                val rn = remoteMessage.notification
                fcmNotif.title = rn!!.title
                fcmNotif.content = rn.body
            }
            loadRetryImageFromUrl(this, fcmNotif, object : CallbackImageNotif {
                override fun onSuccess(bitmap: Bitmap?) {
                    displayNotificationIntent(fcmNotif, bitmap)
                }

                override fun onFailed(string: String?) {
                    displayNotificationIntent(fcmNotif, null)
                }
            })
        }
    }

    private fun displayNotificationIntent(fcmNotif: FcmNotif, bitmap: Bitmap?) {
        playRingtoneVibrate(this)
        var intent = Intent(this, ActivitySplash::class.java)
        if (fcmNotif.place != null) {
            intent = navigateBase(this, fcmNotif.place, true)
        } else if (fcmNotif.news != null) {
            DatabaseHandler(this).refreshTableNewsInfo()
            intent = navigateBase(this, fcmNotif.news, true)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val channelId = getString(R.string.default_notification_channel_id)
        val builder = NotificationCompat.Builder(this, channelId)
        builder.setSmallIcon(R.drawable.ic_notification)
        builder.setContentTitle(fcmNotif.title)
        builder.setContentText(fcmNotif.content)
        builder.setDefaults(Notification.DEFAULT_LIGHTS)
        builder.setAutoCancel(true)
        builder.setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.priority = Notification.PRIORITY_HIGH
        }
        if (bitmap != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle().bigPicture(bitmap)
                    .setSummaryText(fcmNotif.content)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(fcmNotif.content))
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        val unique_id = System.currentTimeMillis().toInt()
        notificationManager.notify(unique_id, builder.build())
    }

    private fun playRingtoneVibrate(context: Context) {
        try {
            // play vibration
            if (sharedPref!!.vibration) {
                (context.getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(VIBRATION_TIME.toLong())
            }
            RingtoneManager.getRingtone(context, Uri.parse(sharedPref!!.ringtone)).play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadRetryImageFromUrl(
        ctx: Context,
        fcmNotif: FcmNotif,
        callback: CallbackImageNotif
    ) {
        var url = ""
        url = if (fcmNotif.place != null) {
            getURLimgPlace(fcmNotif.place!!.image)
        } else if (fcmNotif.news != null) {
            getURLimgNews(fcmNotif.news!!.image!!)
        } else {
            callback.onFailed("")
            return
        }
        glideLoadImageFromUrl(ctx, url, object : CallbackImageNotif {
            override fun onSuccess(bitmap: Bitmap?) {
                callback.onSuccess(bitmap)
            }

            override fun onFailed(string: String?) {
                Log.e("onFailed", "on Failed")
                callback.onFailed("")
            }
        })
    }

    // load image with callback
    var mainHandler = Handler(Looper.getMainLooper())
    var myRunnable: Runnable? = null
    private fun glideLoadImageFromUrl(ctx: Context, url: String, callback: CallbackImageNotif) {
        myRunnable = Runnable {
            Glide.with(ctx).asBitmap().load(url).into(object : SimpleTarget<Bitmap?>() {


                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    callback.onFailed("On Load Failed")
                    mainHandler.removeCallbacks(myRunnable!!)
                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?
                ) {
                    callback.onSuccess(resource)
                    mainHandler.removeCallbacks(myRunnable!!)
                }
            })
        }
        mainHandler.post(myRunnable!!)
    }

    interface CallbackImageNotif {
        fun onSuccess(bitmap: Bitmap?)
        fun onFailed(string: String?)
    }

    companion object {
        private const val VIBRATION_TIME = 500 // in millisecond
    }
}