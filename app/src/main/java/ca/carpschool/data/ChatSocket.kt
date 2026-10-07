package ca.carpschool.data

import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import org.json.JSONObject

sealed interface ChatEvent {
    data class NewMessage(val m: Message) : ChatEvent
    data class NewProposal(val p: Proposal) : ChatEvent
    data class Locked(val driveId: String) : ChatEvent
    data class Error(val message: String) : ChatEvent
}

/** Socket.IO connection for one negotiation. Auth uses the school session token. */
class ChatSocket(private val negotiationId: String) {
    val live = MutableStateFlow("connecting")
    val events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 32)
    private var socket: Socket? = null
    private var pending: CompletableDeferred<JsonElement?>? = null

    private fun parse(a: Any?): JsonElement? = a?.let { runCatching { Net.json.parseToJsonElement(it.toString()) }.getOrNull() }

    suspend fun connect() {
        val school = Carp.school ?: return
        val token = Carp.token()
        val opts = IO.Options().apply {
            transports = arrayOf("websocket")
            auth = mapOf("token" to token)
            reconnection = true
        }
        val s = IO.socket(school.baseUrl, opts)
        socket = s
        s.on(Socket.EVENT_CONNECT) {
            s.emit("negotiation:join", JSONObject().put("negotiationId", negotiationId), Ack { args ->
                val ok = (parse(args.firstOrNull()) as? JsonObject)?.get("ok")?.jsonPrimitive?.booleanOrNull
                live.value = if (ok == false) "offline" else "live"
            })
        }
        s.on(Socket.EVENT_DISCONNECT) { live.value = "offline" }
        s.on(Socket.EVENT_CONNECT_ERROR) { live.value = "offline" }
        s.on("exception") { args ->
            val msg = ((parse(args.firstOrNull()) as? JsonObject)?.get("message") as? JsonPrimitive)?.content ?: "Request rejected"
            val p = pending
            if (p != null && !p.isCompleted) p.completeExceptionally(ApiError(msg, 400)) else events.tryEmit(ChatEvent.Error(msg))
        }
        s.on("message:new") { a -> parse(a.firstOrNull())?.let { runCatching { events.tryEmit(ChatEvent.NewMessage(Net.json.decodeFromJsonElement(it))) } } }
        s.on("proposal:new") { a -> parse(a.firstOrNull())?.let { runCatching { events.tryEmit(ChatEvent.NewProposal(Net.json.decodeFromJsonElement(it))) } } }
        s.on("carpool:locked") { a -> (parse(a.firstOrNull()) as? JsonObject)?.get("driveId")?.jsonPrimitive?.content?.let { events.tryEmit(ChatEvent.Locked(it)) } }
        s.connect()
    }

    suspend fun emit(event: String, data: JsonObject): JsonElement? {
        val s = socket
        if (s == null || !s.connected()) throw ApiError("Not connected. Trying again shortly.", 0)
        val d = CompletableDeferred<JsonElement?>()
        pending = d
        val payload = JSONObject(JsonObject(data + ("negotiationId" to JsonPrimitive(negotiationId))).toString())
        s.emit(event, payload, Ack { args ->
            val a = parse(args.firstOrNull()) as? JsonObject
            when {
                a == null -> d.complete(null)
                a["ok"]?.jsonPrimitive?.booleanOrNull == false || a["error"] != null -> {
                    val e = a["error"]
                    val msg = (e as? JsonObject)?.get("message")?.jsonPrimitive?.content ?: (e as? JsonPrimitive)?.content ?: "Failed"
                    d.completeExceptionally(ApiError(msg, 400))
                }
                else -> d.complete(a["data"] ?: a)
            }
        })
        return try { withTimeout(10_000) { d.await() } } finally { pending = null }
    }

    fun close() { socket?.off(); socket?.disconnect(); socket = null }
}
