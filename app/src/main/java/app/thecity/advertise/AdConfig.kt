package app.thecity.advertise

import app.thecity.advertise.AdConfig
import app.thecity.advertise.AdConfig.AdNetworkType
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import java.io.Serializable
import java.lang.Boolean

object AdConfig : Serializable {
    // flag for display ads
    // flag for enable/disable all ads
    const val ADS_ENABLE = true
    const val ADS_MAIN_BANNER = true
    const val ADS_MAIN_INTERSTITIAL = true
    const val ADS_PLACE_DETAILS_BANNER = true
    const val ADS_NEWS_DETAILS_BANNER = true

    // if you not use ads you can set this to false
    const val ENABLE_GDPR = true
    @JvmField
    var ad_enable = ADS_ENABLE && true
    @JvmField
    var ad_network = AdNetworkType.UNITY
    @JvmField
    var ad_inters_interval = 5
    @JvmField
    var ad_admob_publisher_id = ""
    @JvmField
    var ad_admob_banner_unit_id = ""
    @JvmField
    var ad_admob_interstitial_unit_id = ""
    @JvmField
    var ad_fan_banner_unit_id = ""
    @JvmField
    var ad_fan_interstitial_unit_id = ""
    @JvmField
    var ad_applovin_banner_unit_id = ""
    @JvmField
    var ad_applovin_interstitial_unit_id = ""
    @JvmField
    var ad_unity_game_id = "4768795"  // travvy app id on unity
    @JvmField
    var ad_unity_banner_unit_id = "Banner_Android"
    @JvmField
    var ad_unity_interstitial_unit_id = "Interstitial_Android"

    // Set data from remote config
    fun setFromRemoteConfig(remote: FirebaseRemoteConfig) {
        if (!remote.getString("ad_enable").isEmpty()) ad_enable =
            Boolean.parseBoolean(remote.getString("ad_enable"))
        if (!remote.getString("ad_network").isEmpty()) ad_network =
            AdNetworkType.valueOf(remote.getString("ad_network"))
        if (!remote.getString("ad_inters_interval").isEmpty()) ad_inters_interval =
            remote.getString("ad_inters_interval").toInt()
        if (!remote.getString("ad_admob_publisher_id").isEmpty()) ad_admob_publisher_id =
            remote.getString("ad_inters_interval")
        if (!remote.getString("ad_admob_banner_unit_id").isEmpty()) ad_admob_banner_unit_id =
            remote.getString("ad_admob_banner_unit_id")
        if (!remote.getString("ad_admob_interstitial_unit_id")
                .isEmpty()
        ) ad_admob_interstitial_unit_id = remote.getString("ad_admob_interstitial_unit_id")
        if (!remote.getString("ad_fan_banner_unit_id").isEmpty()) ad_fan_banner_unit_id =
            remote.getString("ad_fan_banner_unit_id")
        if (!remote.getString("ad_fan_interstitial_unit_id").isEmpty()) ad_fan_banner_unit_id =
            remote.getString("ad_fan_banner_unit_id")
        if (!remote.getString("ad_applovin_banner_unit_id").isEmpty()) ad_applovin_banner_unit_id =
            remote.getString("ad_applovin_banner_unit_id")
        if (!remote.getString("ad_applovin_interstitial_unit_id")
                .isEmpty()
        ) ad_applovin_interstitial_unit_id = remote.getString("ad_applovin_interstitial_unit_id")
        if (!remote.getString("ad_unity_game_id").isEmpty()) ad_unity_game_id =
            remote.getString("ad_unity_game_id")
        if (!remote.getString("ad_unity_banner_unit_id").isEmpty()) ad_unity_banner_unit_id =
            remote.getString("ad_unity_banner_unit_id")
        if (!remote.getString("ad_unity_interstitial_unit_id")
                .isEmpty()
        ) ad_unity_interstitial_unit_id = remote.getString("ad_unity_interstitial_unit_id")
    }

    enum class AdNetworkType {
        ADMOB, FAN, APPLOVIN, UNITY
    }
}