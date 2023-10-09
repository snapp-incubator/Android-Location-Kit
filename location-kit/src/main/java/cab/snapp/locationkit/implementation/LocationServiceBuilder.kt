package cab.snapp.locationkit.implementation

import android.content.Context
import android.util.Log
import cab.snapp.locationkit.contract.SnappLocationProvider
import cab.snapp.locationkit.exception.NoGmsOrHmsDetectedException
import cab.snapp.locationkit.helper.VendorServiceHelper

class LocationServiceBuilder(
    private val context: Context
) {

    companion object {
        const val UPDATE_INTERVAL = 10000L
        const val TAG = "SnappLocationProvider"
    }

    private var updateInterval: Long = UPDATE_INTERVAL
    private var fastestUpdateInterval: Long = UPDATE_INTERVAL / 2
    private var useDeviceLocationSystemOnly: Boolean = false
    private var freshLocationUpdateCount: Int = 2
    private var mockRealLocationTrashHold: Int = 20
    private var mockDistanceTrashHoldInMeter: Int = 1000

    fun withUpdateInterval(updateInterval: Long) = apply {
        if (updateInterval <= 0L) {
            this.updateInterval = updateInterval
        }
    }

    fun withFastestUpdateInterval(fastestUpdateInterval: Long) = apply {
        if (fastestUpdateInterval <= 0L) {
            this.fastestUpdateInterval = fastestUpdateInterval
        }
    }

    fun forceUsingDeviceLocationSystemOnly(useDeviceLocationSystemOnly: Boolean) = apply {
        this.useDeviceLocationSystemOnly = useDeviceLocationSystemOnly
    }

    fun freshLocationUpdateCount(value: Int) = apply {
        this.freshLocationUpdateCount = value
    }

    fun mockRealLocationTrashHold(value: Int) = apply {
        this.mockRealLocationTrashHold = value
    }

    fun mockDistanceTrashHoldInMeter(value: Int) = apply {
        this.mockDistanceTrashHoldInMeter = value
    }


    @Throws(NoGmsOrHmsDetectedException::class)
    fun build(): SnappLocationProvider {

        Log.d(TAG, "Start to Build with this config: $this")

        when {
            useDeviceLocationSystemOnly.not() && VendorServiceHelper.isGooglePlayServiceAvailable(context) -> {
                Log.d(TAG, "build: Detected as GMS device")
                return GooglePlaySnappLocationProvider(
                    context,
                    updateInterval,
                    fastestUpdateInterval,
                    freshLocationUpdateCount,
                    mockRealLocationTrashHold,
                    mockDistanceTrashHoldInMeter
                )
            }
            useDeviceLocationSystemOnly.not() && VendorServiceHelper.isHuaweiMobileServiceAvailable(context) -> {
                Log.d(TAG, "build: Detected as HMS device")
                return HmsSnappLocationProvider(
                    context,
                    updateInterval,
                    fastestUpdateInterval,
                    freshLocationUpdateCount,
                    mockRealLocationTrashHold,
                    mockDistanceTrashHoldInMeter
                )
            }
            VendorServiceHelper.isDefaultDeviceLocationSystemAvailable(context) -> {
                Log.d(TAG, "build: Detected default location system on device")
                return DefaultDeviceSnappLocationProvider(
                    context,
                    updateInterval,
                    fastestUpdateInterval,
                    freshLocationUpdateCount,
                    mockRealLocationTrashHold,
                    mockDistanceTrashHoldInMeter
                )
            }
            else -> throw NoGmsOrHmsDetectedException()
        }
    }

    override fun toString(): String {
        return "LocationServiceBuilder(context=$context, updateInterval=$updateInterval, fastestUpdateInterval=$fastestUpdateInterval)"
    }
}