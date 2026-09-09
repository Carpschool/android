package ca.carpschool.data.location

import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * LocationSnapshotHelper
 * 
 * Captures single discrete GPS location snapshots at boarding and dropoff.
 * Strict zero continuous live streaming policy to eliminate battery and server load.
 */
class LocationSnapshotHelper(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    suspend fun captureDiscreteSnapshot(): Location? {
        val cancellationTokenSource = CancellationTokenSource()
        return try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).result
        } catch (e: Exception) {
            null
        }
    }
}
