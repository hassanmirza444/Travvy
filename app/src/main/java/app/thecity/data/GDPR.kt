package app.thecity.data

import android.app.Activity
import android.os.Bundle
import android.util.Log
import app.thecity.BuildConfig
import app.thecity.R
import app.thecity.advertise.AdConfig
import app.thecity.data.GDPR.GDPRForm
import com.google.ads.consent.*
import java.net.MalformedURLException
import java.net.URL

object GDPR {
    fun getBundleAd(act: Activity?): Bundle {
        val extras = Bundle()
        val consentInformation = ConsentInformation.getInstance(act)
        if (consentInformation.consentStatus == ConsentStatus.NON_PERSONALIZED) {
            extras.putString("npa", "1")
        }
        return extras
    }

    fun updateConsentStatus(act: Activity) {
        val consentInformation = ConsentInformation.getInstance(act)
        if (BuildConfig.DEBUG) {
            // How to get device ID : https://goo.gl/2ompNn, https://goo.gl/jrCqfY
            consentInformation.addTestDevice("6E03755720167250AEBF7573B4E86B62")
            consentInformation.debugGeography = DebugGeography.DEBUG_GEOGRAPHY_EEA
        }
        consentInformation.requestConsentInfoUpdate(
            arrayOf(AdConfig.ad_admob_publisher_id),
            object : ConsentInfoUpdateListener {
                override fun onConsentInfoUpdated(consentStatus: ConsentStatus) {
                    // User's consent status successfully updated. Display the consent form if Consent Status is UNKNOWN
                    if (consentStatus == ConsentStatus.UNKNOWN) {
                        GDPRForm(act).displayConsentForm()
                    }
                }

                override fun onFailedToUpdateConsentInfo(errorDescription: String) {
                    // Consent form error.
                    Log.e("GDPR", errorDescription)
                }
            })
    }

    private class GDPRForm(private val activity: Activity) {
        private var form: ConsentForm? = null
        fun displayConsentForm() {
            val builder = ConsentForm.Builder(activity, getUrlPrivacyPolicy(activity))
            builder.withPersonalizedAdsOption()
            builder.withNonPersonalizedAdsOption()
            builder.withListener(object : ConsentFormListener() {
                override fun onConsentFormLoaded() {
                    // Consent form loaded successfully.
                    form!!.show()
                }

                override fun onConsentFormOpened() {
                    // Consent form was displayed.
                }

                override fun onConsentFormClosed(
                    consentStatus: ConsentStatus,
                    userPrefersAdFree: Boolean
                ) {
                    // Consent form was closed.
                    Log.e("GDPR", "Status : $consentStatus")
                }

                override fun onConsentFormError(errorDescription: String) {
                    // Consent form error.
                    Log.e("GDPR", errorDescription)
                }
            })
            form = builder.build()
            form!!.load()
        }

        private fun getUrlPrivacyPolicy(act: Activity): URL? {
            var mUrl: URL? = null
            try {
                mUrl = URL(act.getString(R.string.privacy_policy_url))
            } catch (e: MalformedURLException) {
                Log.e("GDPR", e.message!!)
            }
            return mUrl
        }
    }
}