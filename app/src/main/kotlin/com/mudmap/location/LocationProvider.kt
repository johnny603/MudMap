package com.mudmap.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mudmap.slice1.storage.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationProvider(
    context: Context,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private var request: Job? = null
    private var cached: GeoPoint? = null

    // Called on the main thread when recording starts.
    fun requestOnce() {
        request?.cancel()
        cached = null
        val fine = appContext.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = appContext.checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return
        request = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            cached = try {
                withTimeoutOrNull(10_000L) { currentLocation() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }

    // Freeze this recording's available fix; late results cannot replace it.
    fun last(): GeoPoint? {
        request?.cancel()
        request = null
        return cached
    }

    @Suppress("MissingPermission")
    private suspend fun currentLocation(): GeoPoint? =
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellation.cancel() }
            client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellation.token
            ).addOnSuccessListener { location ->
                if (continuation.isActive) {
                    continuation.resume(
                        location?.let { GeoPoint(it.latitude, it.longitude) }
                    )
                }
            }.addOnFailureListener { error ->
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }.addOnCanceledListener {
                continuation.cancel()
            }
        }
}
