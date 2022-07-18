package app.thecity.model

import java.io.Serializable

class DeviceInfo : Serializable {
    var device: String? = null
    var email: String? = null
    var version: String? = null
    var regid: String? = null
    var date_create: Long = 0

    constructor() {}
    constructor(
        device: String?,
        email: String?,
        version: String?,
        regid: String?,
        date_create: Long
    ) {
        this.device = device
        this.email = email
        this.version = version
        this.regid = regid
        this.date_create = date_create
    }
}