package ca.carpschool.data

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import android.content.Context
import android.content.SharedPreferences
import ca.carpschool.BuildConfig
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import java.time.Instant

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ok<T>(val value: T) : Load<T>
    data class Err(val message: String) : Load<Nothing>
}

/** App-wide session: central registry, chosen school, school session token, current school user. */
object Carp {
    private lateinit var prefs: SharedPreferences
    val central: String get() = BuildConfig.CENTRAL_URL.trimEnd('/')

    private val _schools = MutableStateFlow<Load<List<School>>>(Load.Loading)
    val schools: StateFlow<Load<List<School>>> = _schools.asStateFlow()
    private val _schoolCode = MutableStateFlow<String?>(null)
    val schoolCode: StateFlow<String?> = _schoolCode.asStateFlow()
    private val _meta = MutableStateFlow<SchoolMeta?>(null)
    val meta: StateFlow<SchoolMeta?> = _meta.asStateFlow()
    private val _me = MutableStateFlow<Load<Me?>>(Load.Loading)
    val me: StateFlow<Load<Me?>> = _me.asStateFlow()

    private data class Tok(val code: String, val user: String, val token: String, val exp: Long)
    private var tok: Tok? = null
    private val tokLock = Mutex()

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("carpschool", Context.MODE_PRIVATE)
        _schoolCode.value = prefs.getString("school", null)
    }

    val school: School? get() = (schools.value as? Load.Ok)?.value?.find { it.schoolCode == schoolCode.value }
    val userId: String? get() = Clerk.userFlow.value?.id

    suspend fun loadSchools() {
        _schools.value = Load.Loading
        _schools.value = try {
            Load.Ok(Net.json.decodeFromJsonElement<List<School>>(Net.call("$central/schools") ?: JsonArray(emptyList())))
        } catch (e: Exception) { Load.Err("Couldn't reach the Carpschool network.") }
        school?.let { loadMeta(it) }
    }

    private suspend fun loadMeta(s: School) {
        _meta.value = runCatching { Net.json.decodeFromJsonElement<SchoolMeta>(Net.call(s.baseUrl + "/.well-known/carpschool.json")!!) }.getOrNull()
    }

    suspend fun chooseSchool(s: School?) {
        tok = null
        prefs.edit().apply { if (s == null) remove("school") else putString("school", s.schoolCode) }.apply()
        _schoolCode.value = s?.schoolCode
        _me.value = Load.Loading
        _meta.value = null
        // Run on an app-wide scope: the school picker that called us leaves composition as soon as code changes.
        if (s != null) appScope.launch { loadMeta(s); refreshMe() }
    }

    fun signedOut() { tok = null; _me.value = Load.Loading }

    suspend fun clerkJwt(): String = when (val r = Clerk.auth.getToken()) {
        is ClerkResult.Success -> r.value
        is ClerkResult.Failure -> throw ApiError("You're signed out. Sign in again.", 401)
    }

    suspend fun token(force: Boolean = false): String = tokLock.withLock {
        val s = school ?: throw ApiError("Pick a school first", 400)
        val uid = userId ?: throw ApiError("Signed out", 401)
        val t = tok
        if (!force && t != null && t.code == s.schoolCode && t.user == uid && t.exp - System.currentTimeMillis() > 30_000) return t.token
        val ticket = Net.call("$central/tickets", "POST", buildJsonObject { put("schoolCode", s.schoolCode) }, clerkJwt())!!
            .jsonObject["ticket"]!!.jsonPrimitive.content
        val ses = Net.call(s.baseUrl + "/sessions", "POST", buildJsonObject { put("ticket", ticket) })!!.jsonObject
        val token = ses["token"]!!.jsonPrimitive.content
        val exp = runCatching { Instant.parse(ses["expiresAt"]!!.jsonPrimitive.content).toEpochMilli() }.getOrElse { System.currentTimeMillis() + 600_000 }
        tok = Tok(s.schoolCode, uid, token, exp)
        token
    }

    /** Authenticated call to the chosen school server. Retries once with a fresh ticket on 401. */
    suspend fun api(path: String, method: String = "GET", body: JsonElement? = null): JsonElement? {
        val s = school ?: throw ApiError("Pick a school first", 400)
        return try { Net.call(s.baseUrl + path, method, body, token()) } catch (e: ApiError) {
            if (e.status == 401) Net.call(s.baseUrl + path, method, body, token(force = true)) else throw e
        }
    }

    suspend inline fun <reified T> get(path: String): T = Net.json.decodeFromJsonElement(api(path) ?: JsonNull)
    suspend inline fun <reified T> post(path: String, body: JsonElement? = null): T = Net.json.decodeFromJsonElement(api(path, "POST", body) ?: JsonNull)
    inline fun <reified T> enc(v: T): JsonElement = Net.json.encodeToJsonElement(v)

    suspend fun centralApi(path: String, method: String = "GET", body: JsonElement? = null) = Net.call(central + path, method, body, clerkJwt())

    private val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)

    suspend fun refreshMe(): Me? {
        if (school == null) return null
        // Keep showing the current profile while refreshing, and run on appScope: callers (onboarding
        // steps) leave composition once /me changes, which used to cancel this mid-flight and strand Loading.
        if (_me.value !is Load.Ok) _me.value = Load.Loading
        return appScope.async { fetchMe() }.await()
    }

    private suspend fun fetchMe(): Me? {
        return try {
            val j = api("/me")
            val m = if (j == null || j is JsonNull) null else Net.json.decodeFromJsonElement<Me>(j)
            _me.value = Load.Ok(m); m
        } catch (e: kotlinx.coroutines.CancellationException) { throw e
        } catch (e: Exception) { _me.value = Load.Err(e.message ?: "Couldn't load your profile"); null }
    }

    fun setMe(m: Me) { _me.value = Load.Ok(m) }
}
