package cab.snapp.locationkit.implementation

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import cab.snapp.locationkit.model.NullLocation
import com.huawei.hms.common.ResolvableApiException
import com.huawei.hms.location.*

internal class HmsSnappLocationProvider(
    context: Context,
    updateInterval: Long,
    fastestUpdateInterval: Long,
    freshLocationUpdateCount: Int,
    mockRealLocationTrashHold: Int,
    mockDistanceTrashHoldInMeter: Int
) : BaseSnappLocationProvider(
    context,
    updateInterval,
    fastestUpdateInterval,
    freshLocationUpdateCount,
    mockRealLocationTrashHold,
    mockDistanceTrashHoldInMeter
) {

    private var isLocationUpdatesRequested: Boolean = false

    private val locationSettingsRequest: LocationSettingsRequest by lazy {
        LocationSettingsRequest.Builder().apply {
            addLocationRequest(locationRequest)
        }.build()
    }
    private val locationRequest: LocationRequest by lazy {
        LocationRequest.create().apply {
            interval = updateInterval
            fastestInterval = fastestUpdateInterval
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private val locationRequestFreshOnce: LocationRequest by lazy {
        LocationRequest.create().apply {
            numUpdates = freshLocationUpdateCount
            interval = updateInterval / 2
            fastestInterval = fastestUpdateInterval / 2
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private var settingsClient: SettingsClient = LocationServices.getSettingsClient(context)
    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Create location callback.
     *
     * inside callback method and when every location is ready, the location publish subject will
     * inform changes to its subscribers and also the last location will be updated
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            locationResult?.lastLocation?.let {
                locationIsProvided(it)
            }
        }
    }

    override fun getVendorLocation(callback: (location: Location?) -> Unit) {
        settingsClient.checkLocationSettings(locationSettingsRequest)
            ?.addOnSuccessListener { _ ->
                getLastLocation(callback)
            }
            ?.addOnFailureListener {
                (it as? ResolvableApiException)?.let { e ->
                    if (RESOLUTION_REQUIRED_EXCEPTION == e.message && isLocationInHighAccuracyMode()) {
                        getLastLocation(callback, e)
                    } else {
                        locationStream.accept(
                            NullLocation(
                                LocationManager.GPS_PROVIDER,
                                e
                            )
                        )
                        callback(
                            NullLocation(
                                LocationManager.GPS_PROVIDER,
                                e
                            )
                        )
                    }
                } ?: kotlin.run {
                    locationStream.accept(NullLocation(LocationManager.GPS_PROVIDER))
                    callback(NullLocation(LocationManager.GPS_PROVIDER))
                }
            }
    }

    private fun getLastLocation(callback: (location: Location?) -> Unit, e: Exception? = null) {
        fusedLocationClient.lastLocation
            .addOnCompleteListener { task ->

                // if last location request task was successful then we pass it to the app
                task.takeIf { it.isSuccessful }?.result?.let {
                    locationIsProvided(it, callback)
                } ?: kotlin.run {
                    e?.let {
                        locationStream.accept(
                            NullLocation(
                                LocationManager.GPS_PROVIDER,
                                it
                            )
                        )
                    } ?: kotlin.run {
                        // if by anyhow there isn't any last location, then we trigger
                        // location update to get last location and then stop it after first result
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            object : LocationCallback() {
                                override fun onLocationResult(locationResult: LocationResult?) {
                                    super.onLocationResult(locationResult)
                                    locationIsProvided(
                                        locationResult?.lastLocation,
                                        callback
                                    )
                                    fusedLocationClient.removeLocationUpdates(this)
                                }
                            },
                            Looper.myLooper()
                        )
                    }
                }
            }
    }

    override fun refreshVendorLocation() {
        var remainingUpdateCount = freshLocationUpdateCount

        fusedLocationClient.requestLocationUpdates(
            locationRequestFreshOnce,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    super.onLocationResult(locationResult)
                    remainingUpdateCount--

                    locationIsProvided(
                        locationResult?.lastLocation,
                        null
                    )

                    if (remainingUpdateCount <= 0) {
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            },
            Looper.myLooper()
        )
    }

    override fun stopVendorLocationUpdate() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun startVendorLocationUpdate() {
        settingsClient.checkLocationSettings(locationSettingsRequest)
            ?.addOnSuccessListener { _ ->
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                )
                isLocationUpdatesRequested = true
            }?.addOnFailureListener {
                isLocationUpdatesRequested = false

                (it as? ResolvableApiException)?.let { e ->
                    locationStream.accept(NullLocation(LocationManager.GPS_PROVIDER, e))
                } ?: kotlin.run {
                    locationStream.accept(NullLocation(LocationManager.GPS_PROVIDER))
                }
            }
    }

}