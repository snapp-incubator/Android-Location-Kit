package cab.snapp.locationkit.implementation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import cab.snapp.locationkit.model.NullLocation
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

@SuppressLint("MissingPermission")
internal class GooglePlaySnappLocationProvider(
    context: Context,
    updateInterval: Long,
    fastestUpdateInterval: Long,
    freshLocationUpdateCount: Int,
    mockRealLocationTrashHold: Int,
    mockDistanceTrashHoldInMeter: Int,
) : BaseSnappLocationProvider(
    context,
    updateInterval,
    fastestUpdateInterval,
    freshLocationUpdateCount,
    mockRealLocationTrashHold,
    mockDistanceTrashHoldInMeter
) {

    private var isLocationUpdatesRequested: Boolean = false

    /**
     * Build location settings request.
     *
     *
     * builds location settings request with location request.
     */
    private val locationSettingsRequest: LocationSettingsRequest by lazy {
        LocationSettingsRequest.Builder().apply {
            addLocationRequest(locationRequest)
        }.build()
    }

    /**
     * Create location request
     *
     *
     * creates location request with update_interval, fast_update_interval and priority.
     */
    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, updateInterval)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(fastestUpdateInterval)
            .build()
    }

    /**
     * Create fresh location request
     *
     *
     * creates fresh location request with update_interval, fast_update_interval and num updatesCount priority.
     */
    private val locationRequestFreshOnce: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, updateInterval / 2)
            .setWaitForAccurateLocation(false)
            .setMaxUpdates(freshLocationUpdateCount)
            .setMinUpdateIntervalMillis(fastestUpdateInterval / 2)
            .build()
    }

    /**
     * Create location callback.
     *
     * inside callback method and when every location is ready, the location publish subject will
     * inform changes to its subscribers and also the last location will be updated
     */
    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationIsProvided(locationResult.lastLocation)
        }
    }

    private var settingsClient: SettingsClient = LocationServices.getSettingsClient(context)
    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)


    override fun getVendorLocation(callback: (location: Location?) -> Unit) {
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener { _ ->
                getLastLocation(callback)
            }
            .addOnFailureListener {
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
                } ?: run {
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
                } ?: run {
                    e?.let {
                        locationStream.accept(
                            NullLocation(
                                LocationManager.GPS_PROVIDER,
                                it
                            )
                        )
                    } ?: run {
                        // if by anyhow there isn't any last location, then we trigger
                        // location update to get last location and then stop it after first result
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            object : LocationCallback() {
                                override fun onLocationResult(locationResult: LocationResult) {
                                    super.onLocationResult(locationResult)
                                    locationIsProvided(
                                        locationResult.lastLocation,
                                        callback
                                    )
                                    fusedLocationClient.removeLocationUpdates(this)
                                }
                            },
                            Looper.myLooper()!!
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
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    remainingUpdateCount--

                    locationIsProvided(
                        locationResult.lastLocation,
                        null
                    )

                    if (remainingUpdateCount <= 0) {
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            },
            Looper.myLooper()!!
        )
    }

    override fun stopVendorLocationUpdate() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun startVendorLocationUpdate() {
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener { _ ->
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()!!
                )
                isLocationUpdatesRequested = true
            }.addOnFailureListener {
                isLocationUpdatesRequested = false
                (it as? ResolvableApiException)?.let { e ->
                    locationStream.accept(NullLocation(LocationManager.GPS_PROVIDER, e))
                } ?: kotlin.run {
                    locationStream.accept(NullLocation(LocationManager.GPS_PROVIDER))
                }
            }
    }

}