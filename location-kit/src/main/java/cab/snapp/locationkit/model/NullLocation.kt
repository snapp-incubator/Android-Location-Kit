package cab.snapp.locationkit.model

import android.location.Location
import android.location.LocationManager

open class NullLocation : Location {

    var exception: Exception? = null
    var isBecauseOfPermission: Boolean = false
    var isPermissionPermanentlyDenied: Boolean = false

    constructor() : super(LocationManager.GPS_PROVIDER)

    constructor(provider: String) : super(provider)

    constructor(provider: String, exception: Exception) : super(provider) {
        this.exception = exception
    }

    constructor(provider: String, isBecauseOfPermission: Boolean) : super(provider) {
        this.isBecauseOfPermission = isBecauseOfPermission
    }

    constructor(provider: String, isBecauseOfPermission: Boolean, isPermissionPermanentlyDenied:Boolean) : super(provider) {
        this.isBecauseOfPermission = isBecauseOfPermission
        this.isPermissionPermanentlyDenied = isPermissionPermanentlyDenied
    }

    override fun toString(): String {
        exception?.let {
            return it.toString()
        } ?: kotlin.run {
            return "Null Location is because of permission: $isBecauseOfPermission"
        }
    }
}