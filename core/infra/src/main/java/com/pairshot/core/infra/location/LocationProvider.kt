package com.pairshot.core.infra.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val shortAddress: String?,
)

@Singleton
class LocationProvider
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun getCurrentLocation(): LocationResult? {
        if (!hasLocationPermission()) return null

        val location = getLastLocation() ?: return null
        val addresses = reverseGeocode(location.latitude, location.longitude)

        return LocationResult(
            latitude = location.latitude,
            longitude = location.longitude,
            address = addresses?.first,
            shortAddress = addresses?.second,
        )
    }

    @SuppressWarnings("MissingPermission")
    private suspend fun getLastLocation(): Location? {
        val cached =
            suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        if (cached != null) return cached

        return withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val cancellationToken = CancellationTokenSource()
                cont.invokeOnCancellation { cancellationToken.cancel() }
                fusedClient
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationToken.token)
                    .addOnSuccessListener { location -> cont.resume(location) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }
    }

    private suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(REVERSE_GEOCODE_TIMEOUT_MS) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                                cont.resume(addresses.firstOrNull()?.let { formatAddresses(it) })
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        addresses?.firstOrNull()?.let { formatAddresses(it) }
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }

    private fun formatAddresses(address: android.location.Address): Pair<String, String> {
        val full =
            listOfNotNull(
                address.locality?.takeIf { it.isNotBlank() },
                address.subLocality?.takeIf { it.isNotBlank() },
                address.thoroughfare?.takeIf { it.isNotBlank() },
                address.premises?.takeIf { it.isNotBlank() },
            ).joinToString(" ")
                .ifBlank { address.getAddressLine(0) ?: "" }

        val short = full

        return Pair(full, short)
    }

    companion object {
        private const val CURRENT_LOCATION_TIMEOUT_MS = 8_000L
        private const val REVERSE_GEOCODE_TIMEOUT_MS = 5_000L
    }
}
