package cab.snapp.locationkit.helper

import android.location.Location
import android.os.Build
import cab.snapp.locationkit.model.NullLocation
import com.jakewharton.rxrelay2.BehaviorRelay

internal class MockLocationDetectorHelper(
    private val realLocationTrashHold: Int,
    private val distanceTrashHoldInMeter: Int
) {
    private val stream = BehaviorRelay.create<Boolean>()

    private var currentLocationIsMock = false

    private var lastMockLocation: Location? = null
    private var numGoodReadings = 0

    internal fun getMockLocationStream() : BehaviorRelay<Boolean> {
        return stream
    }

    internal fun onNewLocation(location: Location) {
        if (location is NullLocation) return

        var isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            location.isFromMockProvider
        }

        if (!isMock && location.extras != null && location.extras!!.containsKey("mockLocation")) {
            val extraMock = location.extras!!["mockLocation"] as? Boolean ?: false
            isMock = extraMock
        }

        if (isMock) {
            lastMockLocation = location
            numGoodReadings = 0
        } else {
            numGoodReadings = (numGoodReadings + 1).coerceAtMost(1000000) // Prevent overflow
        }

        if (numGoodReadings >= realLocationTrashHold) lastMockLocation = null

        if (lastMockLocation == null) {
            if (currentLocationIsMock) {
                stream.accept(false)
                currentLocationIsMock = false
            }
            return
        }

        val distance = location.distanceTo(lastMockLocation!!).toDouble()
        val newIsMock = distance < distanceTrashHoldInMeter

        if (newIsMock != currentLocationIsMock) {
            stream.accept(!currentLocationIsMock)
            currentLocationIsMock = !currentLocationIsMock
        }
    }
}