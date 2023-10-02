package cab.snapp.location

import android.content.Context
import android.os.Build
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener

class LocationPermissionHelper(private val context: Context) {

    private fun getLocationPermissionStringArray(): Array<String> {
        return arrayOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
        )
    }

    fun getLocationPermission(permissionCallback: (permitted: Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            permissionCallback(true)
        } else {
            Dexter.withContext(context)
                .withPermissions(*getLocationPermissionStringArray())
                .withListener(object : BaseMultiplePermissionsListener() {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        super.onPermissionsChecked(report)
                        report.takeIf { it.areAllPermissionsGranted() }?.let {
                            permissionCallback(true)
                        } ?: kotlin.run {
                            permissionCallback(false)
                        }
                    }
                })
                .check()
        }
    }


}