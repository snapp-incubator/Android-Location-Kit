package cab.snapp.locationkit.helper

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.huawei.hms.api.HuaweiApiAvailability

object VendorServiceHelper {

    @JvmStatic
    fun isHuaweiMobileServiceAvailable(context: Context): Boolean {
        val apiAvailability = HuaweiApiAvailability.getInstance()
        return apiAvailability.isHuaweiMobileServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    @JvmStatic
    fun isGooglePlayServiceAvailable(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        return googleApiAvailability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    @JvmStatic
    fun isDefaultDeviceLocationSystemAvailable(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION)
    }
}