package ca.carpschool.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** Google mobile REST identity requires uppercase SHA-1 hex without delimiters. */
internal fun certificateSha1Header(certificate: ByteArray): String =
    java.security.MessageDigest.getInstance("SHA-1").digest(certificate)
        .joinToString("") { "%02X".format(java.util.Locale.ROOT, it.toInt() and 0xff) }

internal fun routesRequest(body: String, apiKey: String, packageName: String, certificate: String): okhttp3.Request =
    okhttp3.Request.Builder().url("https://routes.googleapis.com/directions/v2:computeRoutes")
        .header("X-Goog-Api-Key", apiKey)
        .header("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline")
        .header("X-Android-Package", packageName)
        .header("X-Android-Cert", certificate)
        .post(body.toRequestBody("application/json".toMediaType()))
        .build()
