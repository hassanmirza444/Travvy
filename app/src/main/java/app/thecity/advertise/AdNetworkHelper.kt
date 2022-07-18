package app.thecity.advertise

import android.app.Activity
import android.content.Context
import app.thecity.data.SharedPref
import com.applovin.mediation.ads.MaxInterstitialAd
import app.thecity.advertise.AdConfig
import app.thecity.data.GDPR
import android.widget.LinearLayout
import com.google.ads.mediation.admob.AdMobAdapter
import app.thecity.advertise.AdNetworkHelper
import com.facebook.ads.AdView.AdViewLoadConfig
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.BannerView.IListener
import com.unity3d.services.banners.BannerErrorInfo
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.applovin.mediation.MaxAdListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds.UnityAdsShowError
import com.unity3d.ads.UnityAds.UnityAdsShowCompletionState
import android.view.Display
import android.util.DisplayMetrics
import com.unity3d.services.banners.UnityBannerSize
import android.os.Bundle
import android.util.Log
import com.google.ads.mediation.facebook.FacebookExtras
import com.google.ads.mediation.facebook.FacebookAdapter
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.facebook.ads.AdSettings.IntegrationErrorMode
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds.UnityAdsInitializationError
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinMediationProvider
import android.util.TypedValue
import android.view.View
import app.thecity.BuildConfig
import app.thecity.R
import com.facebook.ads.*
import com.facebook.ads.AdError
import com.google.android.gms.ads.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import java.lang.Exception
import java.util.*

class AdNetworkHelper(private val activity: Activity) {
    private val sharedPref: SharedPref

    //Interstitial
    private var adMobInterstitialAd: InterstitialAd? = null
    private var fanInterstitialAd: com.facebook.ads.InterstitialAd? = null
    private var applovinInterstitialAd: MaxInterstitialAd? = null
    fun showGDPR() {
        if (!AdConfig.ad_enable || !AdConfig.ENABLE_GDPR) return
        if (AdConfig.ad_network === AdConfig.AdNetworkType.ADMOB) {
            GDPR.updateConsentStatus(activity)
        }
    }

    fun loadBannerAd(enable: Boolean) {
        if (!AdConfig.ad_enable || !enable) return
        val ad_container = activity.findViewById<LinearLayout>(R.id.ad_container)
        ad_container.removeAllViews()
        if (AdConfig.ad_network === AdConfig.AdNetworkType.ADMOB) {
            val adRequest = AdRequest.Builder().addNetworkExtrasBundle(
                AdMobAdapter::class.java, GDPR.getBundleAd(activity)
            ).build()
            ad_container.visibility = View.GONE
            val adView = AdView(
                activity
            )
            adView.adUnitId = AdConfig.ad_admob_banner_unit_id
            ad_container.addView(adView)
            adView.adSize = admobBannerSize
            adView.loadAd(adRequest)
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    // Code to be executed when an ad finishes loading.
                    ad_container.visibility = View.VISIBLE
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Code to be executed when an ad request fails.
                    ad_container.visibility = View.GONE
                }
            }
        }
        else if (AdConfig.ad_network === AdConfig.AdNetworkType.FAN) {
            val adView = com.facebook.ads.AdView(
                activity,
                AdConfig.ad_fan_banner_unit_id,
                com.facebook.ads.AdSize.BANNER_HEIGHT_50
            )
            // Add the ad view to your activity layout
            ad_container.addView(adView)
            val adListener: com.facebook.ads.AdListener = object : com.facebook.ads.AdListener {
                override fun onError(ad: Ad, adError: AdError) {
                    ad_container.visibility = View.GONE
                    Log.d(
                        TAG,
                        "Failed to load Audience Network : " + adError.errorMessage + " " + adError.errorCode
                    )
                }

                override fun onAdLoaded(ad: Ad) {
                    ad_container.visibility = View.VISIBLE
                }

                override fun onAdClicked(ad: Ad) {}
                override fun onLoggingImpression(ad: Ad) {}
            }
            val loadAdConfig = adView.buildLoadAdConfig().withAdListener(adListener).build()
            adView.loadAd(loadAdConfig)
        }
        else if (AdConfig.ad_network === AdConfig.AdNetworkType.UNITY) {
            val bottomBanner =
                BannerView(activity, AdConfig.ad_unity_banner_unit_id, unityBannerSize)
            bottomBanner.listener = object : IListener {
                override fun onBannerLoaded(bannerView: BannerView) {
                    ad_container.visibility = View.VISIBLE
                    Log.d(TAG, "ready")
                }

                override fun onBannerClick(bannerView: BannerView) {}
                override fun onBannerFailedToLoad(
                    bannerView: BannerView,
                    bannerErrorInfo: BannerErrorInfo
                ) {
                    Log.d(TAG, "Banner Error$bannerErrorInfo")
                    ad_container.visibility = View.GONE
                }

                override fun onBannerLeftApplication(bannerView: BannerView) {}
            }
            ad_container.addView(bottomBanner)
            bottomBanner.load()
        } else if (AdConfig.ad_network === AdConfig.AdNetworkType.APPLOVIN) {
            val maxAdView = MaxAdView(AdConfig.ad_applovin_banner_unit_id, activity)
            maxAdView.setListener(object : MaxAdViewAdListener {
                override fun onAdExpanded(ad: MaxAd) {}
                override fun onAdCollapsed(ad: MaxAd) {}
                override fun onAdLoaded(ad: MaxAd) {
                    ad_container.visibility = View.VISIBLE
                }

                override fun onAdDisplayed(ad: MaxAd) {}
                override fun onAdHidden(ad: MaxAd) {}
                override fun onAdClicked(ad: MaxAd) {}
                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    ad_container.visibility = View.GONE
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {}
            })
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val heightPx = dpToPx(activity, 50)
            maxAdView.layoutParams = FrameLayout.LayoutParams(width, heightPx)
            ad_container.addView(maxAdView)
            maxAdView.loadAd()
        }
    }

    fun loadInterstitialAd(enable: Boolean) {
        if (!AdConfig.ad_enable || !enable) return
        if (AdConfig.ad_network === AdConfig.AdNetworkType.ADMOB) {
            InterstitialAd.load(
                activity,
                AdConfig.ad_admob_interstitial_unit_id,
                getAdRequest(false),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        adMobInterstitialAd = interstitialAd
                        adMobInterstitialAd!!.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    adMobInterstitialAd = null
                                    loadInterstitialAd(enable)
                                }

                                override fun onAdShowedFullScreenContent() {
                                    Log.d(TAG, "The ad was shown.")
                                    sharedPref.intersCounter = 0
                                }
                            }
                        Log.i(TAG, "onAdLoaded")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.i(TAG, loadAdError.message)
                        adMobInterstitialAd = null
                        Log.d(TAG, "Failed load AdMob Interstitial Ad")
                    }
                })
        }
        else if (AdConfig.ad_network === AdConfig.AdNetworkType.FAN) {
            fanInterstitialAd = InterstitialAd(activity, AdConfig.ad_fan_interstitial_unit_id)
            val interstitialAdListener: InterstitialAdListener = object : InterstitialAdListener {
                override fun onInterstitialDisplayed(ad: Ad) {
                    // Interstitial ad displayed callback
                    Log.e(TAG, "Interstitial ad displayed.")
                    sharedPref.intersCounter = 0
                }

                override fun onInterstitialDismissed(ad: Ad) {
                    fanInterstitialAd = null
                    loadInterstitialAd(enable)
                    Log.e(TAG, "Interstitial ad dismissed.")
                }

                override fun onError(ad: Ad, adError: AdError) {
                    Log.e(TAG, "Interstitial ad failed to load: " + adError.errorMessage)
                }

                override fun onAdLoaded(ad: Ad) {
                    Log.d(TAG, "Interstitial ad is loaded and ready to be displayed!")
                }

                override fun onAdClicked(ad: Ad) {
                    Log.d(TAG, "Interstitial ad clicked!")
                }

                override fun onLoggingImpression(ad: Ad) {
                    Log.d(TAG, "Interstitial ad impression logged!")
                }
            }

            // load ads
            fanInterstitialAd!!.loadAd(
                fanInterstitialAd!!.buildLoadAdConfig().withAdListener(interstitialAdListener)
                    .build()
            )
        }
        else if (AdConfig.ad_network === AdConfig.AdNetworkType.UNITY) {
        }
        else if (AdConfig.ad_network === AdConfig.AdNetworkType.APPLOVIN) {
            applovinInterstitialAd =
                MaxInterstitialAd(AdConfig.ad_applovin_interstitial_unit_id, activity)
            applovinInterstitialAd!!.setListener(object : MaxAdListener {
                override fun onAdLoaded(ad: MaxAd) {
                    Log.d(TAG, "AppLovin Interstitial Ad loaded...")
                }

                override fun onAdDisplayed(ad: MaxAd) {
                    sharedPref.intersCounter = 0
                }

                override fun onAdHidden(ad: MaxAd) {
                    applovinInterstitialAd!!.loadAd()
                }

                override fun onAdClicked(ad: MaxAd) {}
                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    Log.d(TAG, "failed to load AppLovin Interstitial : " + error.adLoadFailureInfo)
                    applovinInterstitialAd!!.loadAd()
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    applovinInterstitialAd!!.loadAd()
                }
            })

            // Load the first ad
            applovinInterstitialAd!!.loadAd()
        }
    }

    fun showInterstitialAd(enable: Boolean): Boolean {
        if (!AdConfig.ad_enable || !enable) return false
        val counter = sharedPref.intersCounter
        if (counter > /*AdConfig.ad_inters_interval*/0) {
            if (AdConfig.ad_network === AdConfig.AdNetworkType.ADMOB) {
                if (adMobInterstitialAd == null) return false
                adMobInterstitialAd!!.show(activity)
            } else if (AdConfig.ad_network === AdConfig.AdNetworkType.FAN) {
                if (fanInterstitialAd == null || !fanInterstitialAd!!.isAdLoaded) return false
                fanInterstitialAd!!.show()
            } else if (AdConfig.ad_network === AdConfig.AdNetworkType.UNITY) {
                //if (!UnityAds.isReady(adConfig.ad_unity_interstitial_unit_id)) return false;
                //DataApp.pref().setIntersCounter(0);
                UnityAds.show(
                    activity,
                    AdConfig.ad_unity_interstitial_unit_id,
                    object : IUnityAdsShowListener {
                        override fun onUnityAdsShowFailure(
                            s: String,
                            unityAdsShowError: UnityAdsShowError,
                            s1: String
                        ) {
                            Log.d("Ads Error", unityAdsShowError.name + s)
                        }

                        override fun onUnityAdsShowStart(s: String) {
                            sharedPref.intersCounter = 0
                            loadInterstitialAd(enable)
                        }

                        override fun onUnityAdsShowClick(s: String) {}
                        override fun onUnityAdsShowComplete(
                            s: String,
                            unityAdsShowCompletionState: UnityAdsShowCompletionState
                        ) {
                        }
                    })
            } else if (AdConfig.ad_network === AdConfig.AdNetworkType.APPLOVIN) {
                if (applovinInterstitialAd == null || !applovinInterstitialAd!!.isReady) {
                    return false
                }
                applovinInterstitialAd!!.showAd()
            }
            return true
        } else {
            sharedPref.intersCounter = sharedPref.intersCounter + 1
        }
        return false
    }

    // Step 2 - Determine the screen width (less decorations) to use for the ad width.
    private val admobBannerSize: AdSize
        private get() {
            // Step 2 - Determine the screen width (less decorations) to use for the ad width.
            val display = activity.windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val widthPixels = outMetrics.widthPixels.toFloat()
            val density = outMetrics.density
            val adWidth = (widthPixels / density).toInt()
            // Step 3 - Get adaptive ad size and return for setting on the ad view.
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                activity, adWidth
            )
        }
    private val unityBannerSize: UnityBannerSize
        private get() {
            val display = activity.windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val widthPixels = outMetrics.widthPixels.toFloat()
            val density = outMetrics.density
            val adWidth = (widthPixels / density).toInt()
            return UnityBannerSize(adWidth, 50)
        }

    // for facebook bidding ads
    fun getAdRequest(nativeBanner: Boolean): AdRequest {
        val extras = FacebookExtras().setNativeBanner(nativeBanner).build()
        return if (AdConfig.ENABLE_GDPR) {
            AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, GDPR.getBundleAd(activity))
                .addNetworkExtrasBundle(FacebookAdapter::class.java, extras)
                .build()
        } else {
            AdRequest.Builder()
                .addNetworkExtrasBundle(FacebookAdapter::class.java, extras)
                .build()
        }
    }

    private val gAID: String?
        private get() {
            val idInfo: AdvertisingIdClient.Info
            return try {
                idInfo = AdvertisingIdClient.getAdvertisingIdInfo(activity)
                idInfo.id
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }

    companion object {
        private val TAG = AdNetworkHelper::class.java.simpleName
        @JvmStatic
        fun init(context: Context) {
            if (!AdConfig.ad_enable) return
            if (AdConfig.ad_network === AdConfig.AdNetworkType.ADMOB) {
                // Init firebase ads.
                MobileAds.initialize(context)
            } else if (AdConfig.ad_network === AdConfig.AdNetworkType.FAN) {
                AudienceNetworkAds.initialize(context)
                AdSettings.setIntegrationErrorMode(IntegrationErrorMode.INTEGRATION_ERROR_CALLBACK_MODE)
            } else if (AdConfig.ad_network === AdConfig.AdNetworkType.UNITY) {
                UnityAds.initialize(
                    context,
                    AdConfig.ad_unity_game_id,
                    BuildConfig.DEBUG,
                    object : IUnityAdsInitializationListener {
                        override fun onInitializationComplete() {
                            Log.d(TAG, "Unity Ads Initialization Complete")
                            Log.d(TAG, "Unity Ads Game ID : " + AdConfig.ad_unity_game_id)
                        }

                        override fun onInitializationFailed(
                            error: UnityAdsInitializationError,
                            message: String
                        ) {
                            Log.d(TAG, "Unity Ads Initialization Failed: [$error] $message")
                        }
                    })
            } else if (AdConfig.ad_network === AdConfig.AdNetworkType.APPLOVIN) {
                AppLovinSdk.getInstance(context).mediationProvider = AppLovinMediationProvider.MAX
                AppLovinSdk.getInstance(context).settings.setVerboseLogging(true)
                AppLovinSdk.getInstance(context).settings.testDeviceAdvertisingIds =
                    Arrays.asList("3925c051-4c0a-46c9-902a-766628df6b70")
                AppLovinSdk.getInstance(context).initializeSdk()
                val sdkKey = AppLovinSdk.getInstance(context).sdkKey
                if (sdkKey != context.getString(R.string.applovin_sdk_key)) {
                    Log.e(TAG, "AppLovin ERROR : Please update your sdk key in the manifest file.")
                }
                Log.d(TAG, "AppLovin SDK Key : $sdkKey")
            }
        }

        private fun dpToPx(c: Context, dp: Int): Int {
            val r = c.resources
            return Math.round(
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp.toFloat(),
                    r.displayMetrics
                )
            )
        }
    }

    init {
        sharedPref = SharedPref(activity)
    }
}