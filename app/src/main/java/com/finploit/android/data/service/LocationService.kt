package com.finploit.android.data.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class UserLocation(val lat: Double, val lng: Double, val postalCode: String? = null)

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): UserLocation? {
        return try {
            val cts = CancellationTokenSource()
            val loc = fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
                ?: fusedClient.lastLocation.await()
                ?: return null
            val postal = resolvePostalCode(loc.latitude, loc.longitude)
            UserLocation(loc.latitude, loc.longitude, postal)
        } catch (e: Exception) {
            null
        }
    }

    private fun resolvePostalCode(lat: Double, lng: Double): String? {
        return try {
            @Suppress("DEPRECATION")
            val addresses = Geocoder(context, Locale("pt", "PT")).getFromLocation(lat, lng, 1)
            addresses?.firstOrNull()?.postalCode
        } catch (e: Exception) {
            null
        }
    }
}
