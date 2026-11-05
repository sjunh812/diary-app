package org.sjhstudio.diary.core.platform

import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLPlacemark
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject

/**
 * 위치를 한 번 읽고 기기 지오코더로 동네 이름을 찾는다.
 * CLGeocoder는 iOS 26에서 지원 중단 표시가 됐지만 대체 API가 iOS 26부터라 최소 버전 17에서는 이것을 쓴다.
 */
class IosPlaceFinder : PlaceFinder {
    override suspend fun find(): PlaceLookup = withContext(Dispatchers.Main) {
        val status = CLLocationManager().authorizationStatus
        when {
            !CLLocationManager.locationServicesEnabled() -> PlaceLookup.LocationOff

            status != kCLAuthorizationStatusAuthorizedWhenInUse && status != kCLAuthorizationStatusAuthorizedAlways -> PlaceLookup.Denied

            else -> {
                val mark = withTimeoutOrNull(TIMEOUT_MILLIS) { currentLocation()?.let { placemark(it) } }
                val name = mark?.parts()?.label()
                if (name == null) PlaceLookup.NotFound else PlaceLookup.Found(name)
            }
        }
    }

    // 한국 주소는 subLocality가 동·읍·면이고, 군은 subAdministrativeArea, 시는 locality로 온다.
    private fun CLPlacemark.parts() = PlaceParts(
        countryCode = ISOcountryCode,
        region = administrativeArea,
        city = locality,
        district = subAdministrativeArea,
        street = subLocality
    )

    private suspend fun currentLocation(): CLLocation? = suspendCancellableCoroutine { continuation ->
        val manager = CLLocationManager()
        val listener =
            OneLocation { location ->
                manager.delegate = null
                continuation.resume(location)
            }
        continuation.invokeOnCancellation {
            // 관리자는 대리자를 약하게 잡으니 끝날 때까지 여기서 들고 있는다.
            listener.hashCode()
            manager.stopUpdatingLocation()
        }
        manager.delegate = listener
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.requestLocation()
    }

    private suspend fun placemark(location: CLLocation): CLPlacemark? = suspendCancellableCoroutine { continuation ->
        val geocoder = CLGeocoder()
        continuation.invokeOnCancellation { geocoder.cancelGeocode() }
        geocoder.reverseGeocodeLocation(location) { marks, _ ->
            continuation.resume(marks?.firstOrNull() as? CLPlacemark)
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}

private class OneLocation(private val onResult: (CLLocation?) -> Unit) :
    NSObject(),
    CLLocationManagerDelegateProtocol {
    private var finished = false

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        finish(didUpdateLocations.lastOrNull() as? CLLocation)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        finish(null)
    }

    private fun finish(location: CLLocation?) {
        if (finished) return
        finished = true
        onResult(location)
    }
}
