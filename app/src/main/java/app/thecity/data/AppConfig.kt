package app.thecity.data

object AppConfig {
    // AD NETWORK configuration at advertise/AdConfig.java
    // this flag if you want to hide menu news info
    const val ENABLE_NEWS_INFO = true

    // flag for save image offline
    const val IMAGE_CACHE = true

    // if you place data more than 200 items please set TRUE
    const val LAZY_LOAD = false

    // flag for tracking analytics
    const val ENABLE_ANALYTICS = true

    // clear image cache when receive push notifications
    const val REFRESH_IMG_NOTIF = true

    // when user enable gps, places will sort by distance
    const val SORT_BY_DISTANCE = true

    // distance metric, fill with KILOMETER or MILE only
    const val DISTANCE_METRIC_CODE = "KILOMETER"

    // related to UI display string
    const val DISTANCE_METRIC_STR = "Km"

    // flag for enable disable theme color chooser, in Setting
    const val THEME_COLOR = true
}