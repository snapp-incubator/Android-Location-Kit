package cab.snapp.locationkit.implementation

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import cab.snapp.locationkit.contract.SnappLocationProvider
import cab.snapp.locationkit.helper.MockLocationDetectorHelper
import cab.snapp.locationkit.implementation.LocationServiceBuilder.Companion.TAG
import cab.snapp.locationkit.model.NullLocation
import com.jakewharton.rxrelay2.BehaviorRelay

internal abstract class BaseSnappLocationProvider(
    protected val context: Context,
    protected val updateInterval: Long,
    protected val fastestUpdateInterval: Long,
    protected val freshLocationUpdateCount: Int,
    mockRealLocationTrashHold: Int,
    mockDistanceTrashHoldInMeter: Int,
) : SnappLocationProvider {

    private val mockLocationDetector = MockLocationDetectorHelper(
        mockRealLocationTrashHold,
        mockDistanceTrashHoldInMeter
    )

    private var isLocationUpdatesRequested: Boolean = false
    internal val locationStream = BehaviorRelay.create<Location>()


    companion object {
        internal const val RESOLUTION_REQUIRED_EXCEPTION = "6: RESOLUTION_REQUIRED"
    }

    override fun getLocationStream(): BehaviorRelay<Location> {
        return locationStream
    }

    override fun getMockLocationStream(): BehaviorRelay<Boolean> {
        return mockLocationDetector.getMockLocationStream()
    }

    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    override fun stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates")
        stopVendorLocationUpdate()
        isLocationUpdatesRequested = false
    }

    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    override fun startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates")

        if (isLocationUpdatesRequested) {
            Log.d(TAG, "location updates was requested before!")
            return
        }

        startVendorLocationUpdate()
    }

    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    override fun getLocation(callback: (location: Location?) -> Unit) {
        getVendorLocation(callback)
    }

    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    override fun refreshLocation() {
        refreshVendorLocation()
    }


    abstract fun refreshVendorLocation()

    abstract fun getVendorLocation(callback: (location: Location?) -> Unit)

    abstract fun stopVendorLocationUpdate()

    abstract fun startVendorLocationUpdate()

    /**
     * This function check whether location is on and in high accuracy mode or not
     *
     * calls when ResolvableApiException happens
     */
    internal fun isLocationInHighAccuracyMode(): Boolean {
        return try {
            val locationMode = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE
            )
            return locationMode == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * If any location provided to this method it will save it as last location and pass to all observers
     */
    internal fun locationIsProvided(
        location: Location?,
        callback: ((location: Location?) -> Unit)? = null,
    ) = location?.takeIf { it.latitude.compareTo(0) != 0 && it.longitude.compareTo(0) != 0 }
        ?.let {
            Log.d(TAG, "locationIsProvided: $it")

            // let all subscribers know about it
            mockLocationDetector.onNewLocation(it)
            locationStream.accept(it)

            // pass it to developer
            callback?.let { _ ->
                callback(it)
            }
        } ?: kotlin.run {

        // if the provided location is null then pass NullLocation to developer
        callback?.let { _ ->
            callback(NullLocation(LocationManager.GPS_PROVIDER))
        }
    }

}