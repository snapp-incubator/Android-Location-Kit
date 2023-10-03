
# Snapp Location kit

Snapp Location kit is a library that simplifies the process of working with Android location services. It provides a convenient and easy-to-use API for requesting and receiving location updates, handling permissions, checking location settings, and detecting mock locations. It also supports various location providers, such as fused location, GPS, network, and passive. Android Location Library is a useful tool for developers who want to build location-based applications on Android devices.



## Features

- Support Google location
- Support HMS (huawei) location
- Detect mock locations
- Check and resolve location setting 


## Tech Stack

**Client:** Android


## Screenshots
<img src="https://raw.githubusercontent.com/ali-moghadam/Android-Location-Kit/main/upload/Screenshot_1696363957.png" alt="drawing" width="50%"/>


## How To Use

* Add dependency to **build.gradle**
```kotlin
dependencies {
    implementation("io.github.snapp-incubator:location-kit:[latest-version]")
}
```

Then create an instance of **SnappLocationProvider**

```kotlin
val snappLocationProvider = LocationServiceBuilder(this)
    .withUpdateInterval(10000L)
    .withFastestUpdateInterval(10000L / 2)
    .mockDistanceTrashHoldInMeter(1000)
    .mockRealLocationTrashHold(20)
    .freshLocationUpdateCount(2)
    .forceUsingDeviceLocationSystemOnly(false)
    .build()
```

Start getting locations
```kotlin
snappLocationProvider.startLocationUpdates()
```

Stop getting locations
```kotlin
snappLocationProvider.stopLocationUpdates()
```

Get one-time location
```kotlin
 snappLocationProvider.getLocation { location -> }
```

Get location stream

```kotlin
snappLocationProvider.getLocationStream()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (location is NullLocation) {
                    location.exception?.let { exception ->
                        ResolvableApiExceptionHelper.startResolutionForResult(this, exception, 1001)
                    } ?: run {
                        Toast.makeText(this, "null location", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Do any stuff
                    showLocationDetails(location)
                }
            }
```

To check if the location is **mocked**

```kotlin
snappLocationProvider.getMockLocationStream()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { locationIsMock ->
               // Do any stuff
            }
```


## Authors

- [@ali-moghadam](https://github.com/ali-moghadam/)


## Contributing

Contributions are always welcome!

See `contributing.md` for ways to get started.

