package app.thecity.data

import android.app.Application
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import app.thecity.advertise.AdNetworkHelper.Companion.init
import app.thecity.connection.RestAdapter
import app.thecity.connection.callbacks.CallbackDevice
import app.thecity.utils.Tools
import com.facebook.stetho.Stetho
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ThisApplication : Application() , IUnityAdsInitializationListener {

    private var callback: Call<CallbackDevice>? = null
    private var firebaseAnalytics: FirebaseAnalytics? = null

    /**
     * ---------------------------------------- End of analytics ---------------------------------
     */
    var location: Location? = null
    private var sharedPref: SharedPref? = null
    private var fcm_count = 0
    private val FCM_MAX_COUNT = 5
    override fun onCreate() {
        super.onCreate()
        Log.d(Constant.LOG_TAG, "onCreate : ThisApplication")
        instance = this
        sharedPref = SharedPref(this)

        // initialize firebase
        FirebaseApp.initializeApp(this)
        UnityAds.initialize(this, "4768795", true)
        Stetho.initializeWithDefaults(this)
        // initialize admob
        init(this)

        // obtain regId & registering device to server
        obtainFirebaseToken()

        // activate analytics tracker
        getFirebaseAnalytics()
    }


    @JvmName("setLocation1")
    public fun setLocation(location1: Location) {
        location = location1
    }

    private fun obtainFirebaseToken() {
        fcm_count++
        Log.d("FCM_SUBMIT", "obtainFirebaseToken")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task: Task<String?> ->
            if (!task.isSuccessful) {
                Log.d(
                    "FCM_SUBMIT",
                    "obtainFirebaseToken : " + fcm_count + "-onFailure : " + task.exception!!.message
                )
                if (fcm_count > FCM_MAX_COUNT) return@addOnCompleteListener
                obtainFirebaseToken()
            } else {
                // Get new FCM registration token
                val token = task.result
                Log.d("FCM_SUBMIT", "obtainFirebaseToken : " + fcm_count + "onSuccess")
                sharedPref!!.fcmRegId = token
                if (!TextUtils.isEmpty(token)) sendRegistrationToServer(token)
            }
        }
    }

    /**
     * --------------------------------------------------------------------------------------------
     * For Firebase Cloud Messaging
     */
    private fun sendRegistrationToServer(token: String?) {
        if (Tools.cekConnection(this) && !TextUtils.isEmpty(token)) {
            val api = RestAdapter.createAPI()
            val deviceInfo = Tools.getDeviceInfo(this)
            deviceInfo.regid = token
            callback = api.registerDevice(deviceInfo)
            callback!!.enqueue(object : Callback<CallbackDevice?> {
                override fun onResponse(
                    call: Call<CallbackDevice?>,
                    response: Response<CallbackDevice?>
                ) {
                    response.body()
                }

                override fun onFailure(call: Call<CallbackDevice?>, t: Throwable) {}
            })
        }
    }

    /**
     * --------------------------------------------------------------------------------------------
     * For Google Analytics
     */
    @Synchronized
    fun getFirebaseAnalytics(): FirebaseAnalytics? {
        if (firebaseAnalytics == null && AppConfig.ENABLE_ANALYTICS) {
            // Obtain the Firebase Analytics.
            firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        }
        return firebaseAnalytics
    }

    fun trackScreenView(event: String) {
        var event = event
        if (firebaseAnalytics == null || !AppConfig.ENABLE_ANALYTICS) return
        val params = Bundle()
        event = event.replace("[^A-Za-z0-9_]".toRegex(), "")
        params.putString("event", event)
        firebaseAnalytics!!.logEvent(event, params)
    }

    fun trackEvent(category: String, action: String, label: String) {
        var category = category
        var action = action
        var label = label
        if (firebaseAnalytics == null || !AppConfig.ENABLE_ANALYTICS) return
        val params = Bundle()
        category = category.replace("[^A-Za-z0-9_]".toRegex(), "")
        action = action.replace("[^A-Za-z0-9_]".toRegex(), "")
        label = label.replace("[^A-Za-z0-9_]".toRegex(), "")
        params.putString("category", category)
        params.putString("action", action)
        params.putString("label", label)
        firebaseAnalytics!!.logEvent("EVENT", params)
    }

    public companion object {


        @get:Synchronized
        public var instance: ThisApplication? = null


    }

    override fun onInitializationComplete() {

    }

    override fun onInitializationFailed(p0: UnityAds.UnityAdsInitializationError?, p1: String?) {
        Log.d("Ads Error", p0!!.name + p1)
    }
}