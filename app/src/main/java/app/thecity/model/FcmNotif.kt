package app.thecity.model

import app.thecity.model.Place
import app.thecity.model.NewsInfo
import java.io.Serializable

class FcmNotif : Serializable {
    var title: String? = null
    var content: String? = null
    var type: String? = null
    var place: Place? = null
    var news: NewsInfo? = null
}