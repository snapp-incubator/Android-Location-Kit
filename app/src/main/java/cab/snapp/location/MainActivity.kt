package cab.snapp.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cab.snapp.location.databinding.ActivityMainBinding
import cab.snapp.locationkit.contract.SnappLocationProvider
import cab.snapp.locationkit.helper.ResolvableApiExceptionHelper
import cab.snapp.locationkit.implementation.LocationServiceBuilder
import cab.snapp.locationkit.model.NullLocation
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!

    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private var locationSub: Disposable? = null
    private var locationMockSub: Disposable? = null
    private lateinit var snappLocationProvider: SnappLocationProvider
    private var lastLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationPermissionHelper = LocationPermissionHelper(this)

        snappLocationProvider = LocationServiceBuilder(this)
            .withUpdateInterval(10000L)
            .withFastestUpdateInterval(10000L / 2)
            .mockDistanceTrashHoldInMeter(1000)
            .mockRealLocationTrashHold(20)
            .freshLocationUpdateCount(2)
            .forceUsingDeviceLocationSystemOnly(false)
            .build()

        locationSub = snappLocationProvider.getLocationStream()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { location ->
                if (location is NullLocation) {
                    location.exception?.let { exception ->
                        ResolvableApiExceptionHelper.startResolutionForResult(this, exception, 1001)
                    } ?: run {
                        Toast.makeText(this, "null location", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    lastLocation = location
                    showLocationDetails(location)
                }
            }

        locationMockSub = snappLocationProvider.getMockLocationStream()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                binding.locationDetails.locationStatus.text =
                    if (it) getString(R.string.mock_location) else getString(R.string.real_location)
            }

        binding.buttonStart.setOnClickListener {
            startGettingLocation()
        }

        binding.buttonStop.setOnClickListener {
            stopGettingLocation()
        }

        binding.buttonGetLocation.setOnClickListener {
            getLocationOneTime()
        }
    }

    private fun showLocationDetails(location: Location) {
        binding.locationDetails.accuracy.text = location.accuracy.toString()
        binding.locationDetails.latitude.text = location.latitude.toString()
        binding.locationDetails.longitude.text = location.longitude.toString()
        binding.locationDetails.bearing.text = location.bearing.toString()
        binding.locationDetails.speed.text = location.speed.toString()
        binding.locationDetails.provider.text = location.provider
    }

    override fun onDestroy() {
        snappLocationProvider.stopLocationUpdates()
        locationSub?.takeUnless { it.isDisposed }?.dispose()
        locationMockSub?.takeUnless { it.isDisposed }?.dispose()
        _binding = null
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    fun startGettingLocation() {
        locationPermissionHelper.getLocationPermission {
            binding.locationDetails.serviceStatus.text = getString(R.string.started)
            snappLocationProvider.startLocationUpdates()
        }
    }

    @SuppressLint("SetTextI18n")
    fun stopGettingLocation() {
        binding.locationDetails.serviceStatus.text = getString(R.string.stopped)
        snappLocationProvider.stopLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    fun getLocationOneTime() {
        locationPermissionHelper.getLocationPermission {
            snappLocationProvider.getLocation { location ->
                location?.let {
                    showLocationDetails(it)
                }
            }
        }
    }
}