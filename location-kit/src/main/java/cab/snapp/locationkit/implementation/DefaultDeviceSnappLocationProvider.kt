package cab.snapp.locationkit.implementation

import android.annotation.SuppressLint
import android.content.Context
import android.location.*
import android.os.Build
import android.os.Looper
import androidx.core.location.LocationManagerCompat
import cab.snapp.locationkit.model.NullLocation
import java.util.concurrent.Executors

@SuppressLint("MissingPermission","WrongConstant")
internal class DefaultDeviceSnappLocationProvider(
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

    companion object {
        const val TEN_MIN_IN_MILLI_SECONDS = 10 * 60 * 1000
    }
    private var isLocationUpdatesRequested: Boolean = false
    private var locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var sat_count = 0
    private var providerInUse : String? = null
    private val uselessProviders = mutableListOf<String>()
    private val locationListener : LocationListener by lazy {
        LocationListener {
            locationIsProvided(it)
            updateSatellitesCount(it)
            if(sat_count == 0) {
                retryWithMoreAccurateProvider()
            }
            if(isBestProviderInUse().not()) {
                resetVendorLocationUpdate()
            }
        }
    }
    private val singleRetryLocationListener by lazy { LocationListener {
        updateSatellitesCount(it)
    } }

    private fun isLocationEnabled() : Boolean {
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    override fun refreshVendorLocation() {
        uselessProviders.clear()
        if(isLocationEnabled()) {
            try {
                for (provider in locationManager.allProviders) {
                    if(locationManager.isProviderEnabled(provider)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            locationManager.getCurrentLocation(provider,
                                null,
                                Executors.newSingleThreadExecutor()
                            ) {

                            }
                        } else {
                            locationManager.requestSingleUpdate(
                                provider,
                                LocationListener {

                                },
                                Looper.myLooper()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                locationStream.accept(
                    NullLocation(
                        LocationManager.GPS_PROVIDER,
                        e
                    )
                )
            }
        } else {
            locationStream.accept(
                NullLocation(
                    LocationManager.GPS_PROVIDER,
                    IllegalStateException("Location is not enabled!!")
                )
            )
        }
    }

    override fun getVendorLocation(callback: (location: Location?) -> Unit) {
        if(isLocationEnabled()) {
            try {
                val locations = mutableListOf<Location?>()
                for (provider in locationManager.allProviders) {
                    if(locationManager.isProviderEnabled(provider)) {
                        val location = locationManager.getLastKnownLocation(provider)
                        if (location != null) {
                            if(System.currentTimeMillis() - location.time <= TEN_MIN_IN_MILLI_SECONDS) {
                                locations.add(location)
                                uselessProviders.remove(provider)
                            } else {
                                uselessProviders.add(provider)
                            }
                        } else {
                            uselessProviders.add(provider)
                        }
                    }
                }
                if(uselessProviders.size == locationManager.allProviders.size) {
                    throw NoSuchElementException()
                } else {
                    var bestLocation: Location? = null
                    for (location in locations) {
                        if(location != null) {
                            if(bestLocation == null) {
                                bestLocation = location
                            } else {
                                if(bestLocation.accuracy > location.accuracy) {
                                    bestLocation = location
                                }
                            }
                        }
                    }
                    if(bestLocation != null) {
                        locationIsProvided(
                            bestLocation,
                            callback
                        )
                    } else {
                        throw Exception()
                    }
                }
            } catch (e: Exception) {
                if (e is NoSuchElementException) {
                    refreshVendorLocation()
                    getVendorLocation(callback)
                } else {
                    locationStream.accept(
                        NullLocation(
                            LocationManager.GPS_PROVIDER,
                            e
                        )
                    )
                }
            }
        } else {
            locationStream.accept(
                NullLocation(
                    LocationManager.GPS_PROVIDER,
                    IllegalStateException("Location is not enabled!!")
                )
            )
        }
    }

    override fun stopVendorLocationUpdate() {
        if(isLocationUpdatesRequested.not()) return
        /**
         * We don't need to call this method for now. maybe later we need it. So we decided to make it commented
         */
//        removeGpsListeners()
        locationManager.removeUpdates(locationListener)
        providerInUse = null
        isLocationUpdatesRequested = false
    }

    @SuppressLint("MissingPermission")
    override fun startVendorLocationUpdate() {
        if(isLocationUpdatesRequested) return
        if(isLocationEnabled()) {
            try {
                val locationProvider = getBestProvider()
                if(locationProvider.isNotEmpty()) {
                    isLocationUpdatesRequested = true
                    providerInUse = locationProvider
                    locationManager.requestLocationUpdates(locationProvider,updateInterval,0f,locationListener)
                } else {
                    isLocationUpdatesRequested = false
                    providerInUse = null
                }
            } catch (e: Exception) {
                providerInUse = null
                isLocationUpdatesRequested = false
                locationStream.accept(
                    NullLocation(
                        LocationManager.GPS_PROVIDER,
                        e
                    )
                )
            }
        } else {
            providerInUse = null
            isLocationUpdatesRequested = false
            locationStream.accept(
                NullLocation(
                    LocationManager.GPS_PROVIDER,
                    IllegalStateException("Location is not enabled!!")
                )
            )
        }
    }

    private fun resetVendorLocationUpdate() {
        stopVendorLocationUpdate()
        startVendorLocationUpdate()
    }

    private fun isGpsEnabled(): Boolean {
        return sat_count >= 4 && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun isNetworkEnabled() : Boolean {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun isBestProviderInUse() : Boolean {
        return providerInUse == getBestProvider()
    }

    private fun getBestProvider() : String {
        return when {
            isGpsEnabled() -> {
                LocationManager.GPS_PROVIDER
            }
            isNetworkEnabled() -> {
                LocationManager.NETWORK_PROVIDER
            }
            else -> {
                val criteria = Criteria()
                criteria.accuracy = Criteria.ACCURACY_FINE
                criteria.isAltitudeRequired = false
                criteria.isBearingRequired = true
                criteria.isCostAllowed = true
                criteria.isSpeedRequired = false
                criteria.powerRequirement = Criteria.POWER_HIGH
                locationManager.getBestProvider(criteria, true) ?: LocationManager.PASSIVE_PROVIDER
            }
        }
    }

    private fun updateSatellitesCount(location: Location) {
        sat_count = if(location.provider == LocationManager.GPS_PROVIDER) {
            location.extras?.getInt("satellites") ?: 0
        } else {
            0
        }
    }

    private fun retryWithMoreAccurateProvider() {
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER,
                    null,
                    Executors.newSingleThreadExecutor()
                ) {
                    updateSatellitesCount(it)
                }
            } else {
                locationManager.removeUpdates(singleRetryLocationListener)
                locationManager.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    singleRetryLocationListener,
                    Looper.myLooper()
                )
            }
        }
    }
}