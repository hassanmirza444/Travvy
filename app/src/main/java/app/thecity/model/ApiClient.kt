package app.thecity.model

import app.thecity.model.Place
import app.thecity.model.PlaceCategory
import java.io.Serializable
import java.util.ArrayList

class ApiClient : Serializable {
    var places: List<Place> = ArrayList()
    var place_category: List<PlaceCategory> = ArrayList()
    var images: List<Images> = ArrayList()

    constructor() {}
    constructor(places: List<Place>, place_category: List<PlaceCategory>, images: List<Images>) {
        this.places = places
        this.place_category = place_category
        this.images = images
    }
}