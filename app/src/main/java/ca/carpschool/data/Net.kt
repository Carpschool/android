package ca.carpschool.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiError(message: String, val status: Int) : Exception(message)

object Net {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true; encodeDefaults = true }
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
    private val JSON = "application/json".toMediaType()

    suspend fun call(url: String, method: String = "GET", body: JsonElement? = null, bearer: String? = null): JsonElement? =
        withContext(Dispatchers.IO) {
            val b = Request.Builder().url(url)
            bearer?.let { b.header("Authorization", "Bearer $it") }
            val rb = body?.toString()?.toRequestBody(JSON)
            when (method) {
                "GET" -> b.get()
                "DELETE" -> if (rb != null) b.delete(rb) else b.delete()
                else -> b.method(method, rb ?: "{}".toRequestBody(JSON))
            }
            val res = try { http.newCall(b.build()).execute() } catch (e: Exception) {
                throw ApiError("Can't reach the server. Check your connection.", 0)
            }
            res.use { r ->
                val text = r.body.string()
                val j = runCatching { if (text.isBlank()) null else json.parseToJsonElement(text) }.getOrNull()
                if (!r.isSuccessful) throw ApiError(errorText(j, r.code, r.message), r.code)
                j
            }
        }

    private fun errorText(j: JsonElement?, code: Int, fallback: String): String {
        if (code == 429) return "Too many tries. Wait a minute and try again."
        val o = j as? JsonObject ?: return fallback.ifBlank { "Request failed ($code)" }
        val m = o["message"] ?: o["error"]
        return when (m) {
            is JsonPrimitive -> m.content
            is JsonArray -> m.joinToString(". ") { (it as? JsonPrimitive)?.content ?: it.toString() }
            is JsonObject -> {
                val form = (m["formErrors"] as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()
                val fields = (m["fieldErrors"] as? JsonObject)?.map { (k, v) -> "$k: " + ((v as? JsonArray)?.firstOrNull()?.jsonPrimitive?.content ?: "") } ?: emptyList()
                (form + fields).joinToString(". ").ifBlank { "Invalid input" }
            }
            else -> fallback.ifBlank { "Request failed ($code)" }
        }
    }
}
