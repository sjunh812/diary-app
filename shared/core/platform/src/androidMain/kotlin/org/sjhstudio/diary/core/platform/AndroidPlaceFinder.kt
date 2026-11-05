package org.sjhstudio.diary.core.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 위치를 한 번 읽고 기기 지오코더로 동네 이름을 찾는다.
 * 대략 위치만 허용돼도 그대로 쓴다.
 */
class AndroidPlaceFinder(private val context: Context) : PlaceFinder {
    override suspend fun find(): PlaceLookup {
        if (!hasPermission()) return PlaceLookup.Denied
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null || !LocationManagerCompat.isLocationEnabled(manager)) return PlaceLookup.LocationOff
        if (!Geocoder.isPresent()) return PlaceLookup.NotFound
        val address =
            withTimeoutOrNull(TIMEOUT_MILLIS) {
                currentLocation()?.let { address(it) }
            }
        val name = address?.parts()?.label() ?: return PlaceLookup.NotFound
        return PlaceLookup.Found(name)
    }

    private fun Address.parts() =
        PlaceParts(countryCode = countryCode, region = adminArea, city = locality, district = subLocality, street = thoroughfare)

    private fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission") // find()가 먼저 확인한다.
    private suspend fun currentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cancel = CancellationTokenSource()
        continuation.invokeOnCancellation { cancel.cancel() }
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(REQUEST, cancel.token)
            .addOnCompleteListener { task -> continuation.resume(if (task.isSuccessful) task.result else null) }
    }

    private suspend fun address(location: Location): Address? {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    }
                )
            }
        }
        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
            } catch (_: IOException) {
                null
            }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val LOCATE_MILLIS = 8_000L
        const val RECENT_MILLIS = 60_000L

        // 누를 때 한 번만 읽고 끝나니 정확도를 앞세운다. 1분 안에 읽은 값이 있으면 그대로 쓴다.
        val REQUEST: CurrentLocationRequest =
            CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(LOCATE_MILLIS)
                .setMaxUpdateAgeMillis(RECENT_MILLIS)
                .build()
    }
}
