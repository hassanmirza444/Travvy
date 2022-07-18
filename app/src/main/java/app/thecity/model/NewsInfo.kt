package app.thecity.model

import java.io.Serializable

class NewsInfo : Serializable {
    var id = 0
    @JvmField
    var title: String? = null
    var brief_content: String? = null
    var full_content: String? = null
    var image: String? = null
    var last_update: Long = 0
}