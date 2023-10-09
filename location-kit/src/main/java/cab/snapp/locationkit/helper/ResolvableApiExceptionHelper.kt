package cab.snapp.locationkit.helper

import android.app.Activity
import com.google.android.gms.common.api.ResolvableApiException

object ResolvableApiExceptionHelper {

    @JvmStatic
    fun startResolutionForResult(activity: Activity, exception: Exception, requestCode: Int) {
        if (activity.isFinishing) {
            return
        }
        try {
            when (exception) {
                is ResolvableApiException -> {
                    exception.startResolutionForResult(activity, requestCode)
                }
                is ResolvableApiException -> {
                    exception.startResolutionForResult(activity, requestCode)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun isExceptionResolvable(e: Exception): Boolean {
        return when (e) {
            is ResolvableApiException -> {
                true
            }
            is ResolvableApiException -> {
                true
            }
            else -> {
                false
            }
        }
    }

}