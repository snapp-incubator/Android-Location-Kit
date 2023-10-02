package cab.snapp.locationkit.contract

import android.location.Location
import androidx.annotation.RequiresPermission
import com.jakewharton.rxrelay2.BehaviorRelay

/**
 * This contract is for any implementation that works with any third-party or native location service
 * that helps developer to get work with location easier and safer
 */
interface SnappLocationProvider {

    fun getLocationStream(): BehaviorRelay<Location>

    fun getMockLocationStream(): BehaviorRelay<Boolean>

    fun stopLocationUpdates()

    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    @Throws(SecurityException::class)
    fun startLocationUpdates()

    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    @Throws(SecurityException::class)
    fun getLocation(callback: (location: Location?) -> Unit)

    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    @Throws(SecurityException::class)
    fun refreshLocation()
}