package ca.carpschool.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import ca.carpschool.BuildConfig
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

data class Suggestion(val id: String, val main: String, val secondary: String)
data class RouteResult(val coordinates: List<List<Double>>, val meters: Double, val seconds: Double)

object Geo {
    private var session: AutocompleteSessionToken? = null

    suspend fun search(ctx: Context, q: String, bias: LatLng?): List<Suggestion> {
        if (!Places.isInitialized() || q.isBlank()) return emptyList()
        val client = Places.createClient(ctx)
        val tok = session ?: AutocompleteSessionToken.newInstance().also { session = it }
        val req = FindAutocompletePredictionsRequest.builder().setQuery(q).setSessionToken(tok).apply {
            bias?.let { setLocationBias(CircularBounds.newInstance(it, 40000.0)) }
        }.build()
        return client.findAutocompletePredictions(req).await().autocompletePredictions.map {
            Suggestion(it.placeId, it.getPrimaryText(null).toString(), it.getSecondaryText(null).toString())
        }
    }

    suspend fun place(ctx: Context, id: String): Pair<String, LatLng>? {
        val client = Places.createClient(ctx)
        val r = client.fetchPlace(FetchPlaceRequest.builder(id, listOf(Place.Field.DISPLAY_NAME, Place.Field.LOCATION, Place.Field.FORMATTED_ADDRESS)).setSessionToken(session).build()).await()
        session = null
        val loc = r.place.location ?: return null
        return (r.place.displayName ?: r.place.formattedAddress ?: "Home") to loc
    }

    /** Road route between two [lng,lat] points via Google Routes API (computeRoutes), thinned to <= 900 points. */
    suspend fun route(from: List<Double>, to: List<Double>): RouteResult {
        fun wp(p: List<Double>) = buildJsonObject { putJsonObject("location") { putJsonObject("latLng") { put("latitude", p[1]); put("longitude", p[0]) } } }
        val body = buildJsonObject { put("origin", wp(from)); put("destination", wp(to)); put("travelMode", "DRIVE"); put("routingPreference", "TRAFFIC_UNAWARE"); put("polylineQuality", "HIGH_QUALITY") }
        val req = okhttp3.Request.Builder().url("https://routes.googleapis.com/directions/v2:computeRoutes")
            .header("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
            .header("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline")
            .header("X-Android-Package", BuildConfig.APPLICATION_ID)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try { Net.http.newCall(req).execute().use { if (it.isSuccessful) it.body.string() else null } } catch (e: Exception) { null }
        } ?: throw ApiError("Routing is unavailable right now", 502)
        val r = (Net.json.parseToJsonElement(text).jsonObject["routes"] as? JsonArray)?.firstOrNull()?.jsonObject ?: throw ApiError("No driving route found", 404)
        var c = decodePolyline(r["polyline"]!!.jsonObject["encodedPolyline"]!!.jsonPrimitive.content)
        if (c.size > 900) { val step = (c.size + 899) / 900; c = c.filterIndexed { i, _ -> i % step == 0 || i == c.size - 1 } }
        val secs = r["duration"]?.jsonPrimitive?.content?.removeSuffix("s")?.toDoubleOrNull() ?: 0.0
        return RouteResult(c, r["distanceMeters"]?.jsonPrimitive?.doubleOrNull ?: 0.0, secs)
    }

    /** Google encoded polyline to [[lng,lat]] rounded to 6 dp. */
    fun decodePolyline(s: String): List<List<Double>> {
        val out = ArrayList<List<Double>>(); var i = 0; var lat = 0; var lng = 0
        while (i < s.length) {
            for (k in 0..1) {
                var shift = 0; var res = 0; var b: Int
                do { b = s[i++].code - 63; res = res or ((b and 0x1f) shl shift); shift += 5 } while (b >= 0x20)
                val d = if (res and 1 != 0) (res shr 1).inv() else res shr 1
                if (k == 0) lat += d else lng += d
            }
            out.add(listOf(Math.round(lng * 10.0) / 1e6, Math.round(lat * 10.0) / 1e6))
        }
        return out
    }

    fun hasLocation(ctx: Context) = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** One discrete GPS snapshot. Never tracks. Falls back to [fallback] (approximate) if unavailable. */
    @SuppressLint("MissingPermission")
    suspend fun snapshot(ctx: Context, fallback: Point): Pair<Point, Boolean> {
        if (!hasLocation(ctx)) return fallback to true
        val loc = runCatching {
            withTimeoutOrNull(8000) { LocationServices.getFusedLocationProviderClient(ctx).getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await() }
        }.getOrNull() ?: return fallback to true
        return Point.of(loc.latitude, loc.longitude) to false
    }
}

fun Point.latLng() = LatLng(lat, lng)
fun List<Double>.latLng() = LatLng(this[1], this[0])
