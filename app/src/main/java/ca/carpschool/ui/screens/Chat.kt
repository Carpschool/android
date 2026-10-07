package ca.carpschool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ca.carpschool.data.*
import ca.carpschool.ui.components.*
import ca.carpschool.ui.theme.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@Composable
fun ChatsScreen(me: Me, nav: NavController) {
    val q = rememberQuery("negs", pollMs = 15000) { Carp.get<List<Negotiation>>("/negotiations").sortedByDescending { it.updatedAt } }
    Screen {
        item { PageHead("Negotiate pickup", "Chats", "Agree on a spot and time, then accept to lock the carpool.") }
        when (val s = q.state) {
            is Load.Loading -> item { Skeleton(3, 72) }
            is Load.Err -> item { ErrorBox(s.message, q.refresh) }
            is Load.Ok -> {
                if (s.value.isEmpty()) item { EmptyState(Icons.Outlined.ChatBubbleOutline, "No chats yet", if (me.isDriver) "Open a drive and message a matched rider." else "Drivers passing near you will message you here.") }
                itemsIndexed(s.value, key = { _, n -> n.id }) { i, n ->
                    val other = if (me.isDriver) n.rider else n.driver
                    Ticket(Modifier.rise(i), accent = if (n.status == "locked") Moss else if (n.status == "open") Amber else MaterialTheme.colorScheme.outline, onClick = { nav.navigate("chat/${n.id}") }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(null, if (me.isDriver) "R" else "D", 40); Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${if (me.isDriver) "Rider" else "Driver"} ${shortId(other)}", style = MaterialTheme.typography.titleMedium)
                                Text(clock(n.updatedAt).ifBlank { "" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusChip(when (n.status) { "locked" -> "Confirmed"; "open" -> "Open"; else -> n.status.replaceFirstChar { it.uppercase() } }, when (n.status) { "locked" -> "success"; "open" -> "warn"; else -> "default" })
                        }
                    }
                }
            }
        }
    }
}

private sealed interface Item { val at: String; val key: String }
private data class MsgItem(val m: Message) : Item { override val at = m.createdAt; override val key = "m" + m.id }
private data class PropItem(val p: Proposal) : Item { override val at = p.createdAt; override val key = "p" + p.id }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(id: String, me: Me, nav: NavController) {
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val myId = Carp.userId
    val neg = rememberQuery("neg", id) { Carp.get<Negotiation>("/negotiations/$id") }
    val homes = rememberQuery("homes") { Carp.get<List<Home>>("/homes") }
    val messages = remember { mutableStateListOf<Message>() }
    val proposals = remember { mutableStateListOf<Proposal>() }
    var pin by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf("") }
    var propose by remember { mutableStateOf(false) }
    var accept by remember { mutableStateOf<Proposal?>(null) }
    val sock = remember(id) { ChatSocket(id) }
    val live by sock.live.collectAsState()
    var lockedDrive by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        runCatching {
            val m = Carp.get<List<Message>>("/negotiations/$id/messages"); messages.clear(); messages.addAll(m)
            val p = Carp.get<List<Proposal>>("/negotiations/$id/proposals"); proposals.clear(); proposals.addAll(p)
        }.onFailure { toast(errText(it)) }
    }
    LaunchedEffect(id) { reload(); runCatching { sock.connect() } }
    LaunchedEffect(sock) {
        sock.events.collect { e ->
            when (e) {
                is ChatEvent.NewMessage -> if (messages.none { it.id == e.m.id }) messages.add(e.m)
                is ChatEvent.NewProposal -> if (proposals.none { it.id == e.p.id }) proposals.add(e.p) else { val i = proposals.indexOfFirst { it.id == e.p.id }; proposals[i] = e.p }
                is ChatEvent.Locked -> { lockedDrive = e.driveId; neg.refresh(); reload() }
                is ChatEvent.Error -> toast(e.message)
            }
        }
    }
    DisposableEffect(sock) { onDispose { sock.close() } }

    val n = neg.data
    val locked = n?.status == "locked" || lockedDrive != null
    val items = (messages.map { MsgItem(it) } + proposals.map { PropItem(it) }).sortedBy { it.at }
    val list = rememberLazyListState()
    LaunchedEffect(items.size) { if (items.isNotEmpty()) list.animateScrollToItem(items.size - 1) }
    val other = n?.let { if (me.isDriver) it.rider else it.driver }

    Scaffold(topBar = {
        TopAppBar(title = {
            Column {
                Text(if (other != null) "${if (me.isDriver) "Rider" else "Driver"} ${shortId(other)}" else "Chat")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (live == "live") Moss else if (live == "connecting") Amber else Brick))
                    Spacer(Modifier.width(6.dp))
                    Text(when (live) { "live" -> "Live"; "connecting" -> "Connecting"; else -> "Offline, reconnecting" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }, navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
            actions = { if (other != null) SafetyMenu(other, if (me.isDriver) "rider" else "driver") { nav.popBackStack() } })
    }, bottomBar = {
        if (!locked && n?.status == "open") Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { propose = true }) { Icon(Icons.Outlined.AddLocationAlt, "Propose pickup") }
                OutlinedTextField(text, { text = it.take(2000) }, Modifier.weight(1f), placeholder = { Text("Message") }, maxLines = 4, shape = RoundedCornerShape(24.dp))
                IconButton(enabled = text.isNotBlank(), onClick = {
                    val t = text.trim(); text = ""
                    scope.launch {
                        try {
                            val r = if (live == "live") sock.emit("message:send", buildJsonObject { put("text", t) }) else Carp.api("/negotiations/$id/messages", "POST", buildJsonObject { put("text", t) })
                            runCatching { Net.json.decodeFromJsonElement<Message>(r!!) }.getOrNull()?.let { m -> if (messages.none { it.id == m.id }) messages.add(m) }
                        } catch (e: Exception) { text = t; toast(errText(e)) }
                    }
                }) { Icon(Icons.AutoMirrored.Outlined.Send, "Send", tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) }
            }
        }
    }) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p), state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (locked) item {
                InfoBox("Carpool confirmed.${pin?.let { " Your boarding PIN is $it." } ?: ""}", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer) {
                    TextButton(onClick = { nav.navigate("ride/${lockedDrive ?: n?.driveId}") }) { Text("Open ride") }
                }
            }
            if (items.isEmpty()) item { Text("Say hi and propose a pickup spot and time. Either of you can accept the other's proposal.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
            items(items, key = { it.key }) { it ->
                when (it) {
                    is MsgItem -> Bubble(it.m, it.m.author == myId)
                    is PropItem -> ProposalCard(it.p, it.p.author == myId, canAccept = !locked && it.p.author != myId && it.p.status == "pending") { accept = it.p }
                }
            }
        }
    }

    if (propose) ProposeSheet(campusOf(Carp.meta.collectAsState().value), homes.data?.firstOrNull()?.location?.latLng(), onDismiss = { propose = false }) { ll, time ->
        try {
            val body = buildJsonObject { put("pickup", Carp.enc(Point.of(ll.latitude, ll.longitude))); put("time", time) }
            val r = if (live == "live") sock.emit("proposal:send", body) else Carp.api("/negotiations/$id/proposals", "POST", body)
            runCatching { Net.json.decodeFromJsonElement<Proposal>(r!!) }.getOrNull()?.let { pr -> if (proposals.none { it.id == pr.id }) proposals.add(pr) }
            propose = false
        } catch (e: Exception) { toast(errText(e)) }
    }
    accept?.let { pr -> ConfirmDialog("Lock this carpool?", "Pickup at ${pr.time}. This reserves a seat and closes the chat.", "Accept", onDismiss = { accept = null }) {
        try {
            val r = Carp.api("/negotiations/$id/proposals/${pr.id}/accept", "POST") as? JsonObject
            pin = r?.get("pin")?.jsonPrimitive?.contentOrNull
            lockedDrive = r?.get("driveId")?.jsonPrimitive?.contentOrNull
            neg.refresh(); reload(); toast("Carpool locked")
        } catch (e: Exception) { toast(errText(e)) }
        accept = null
    } }
}

@Composable
private fun Bubble(m: Message, mine: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Surface(color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(20.dp, 20.dp, if (mine) 6.dp else 20.dp, if (mine) 20.dp else 6.dp), modifier = Modifier.widthIn(max = 300.dp)) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(m.text, style = MaterialTheme.typography.bodyLarge)
                Text(clock(m.createdAt), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End).padding(top = 2.dp), color = LocalContentColor.current.copy(alpha = .6f))
            }
        }
    }
}

@Composable
private fun ProposalCard(p: Proposal, mine: Boolean, canAccept: Boolean, onAccept: () -> Unit) {
    Ticket(accent = if (p.status == "accepted") Moss else Amber, stub = { Stub("PICKUP", p.time) }) {
        Kicker(if (mine) "You proposed" else "They proposed")
        Spacer(Modifier.height(8.dp))
        MapCard(center = p.pickup.latLng(), zoom = 16f, height = 120.dp, pins = listOf(Pin(p.pickup.latLng(), "Pickup", HUE_PICK)))
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusChip(p.status.replaceFirstChar { it.uppercase() }, if (p.status == "accepted") "success" else if (p.status == "pending") "warn" else "default")
            Spacer(Modifier.weight(1f))
            if (canAccept) Button(onClick = onAccept) { Text("Accept") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProposeSheet(campus: LatLng, start: LatLng?, onDismiss: () -> Unit, onSend: suspend (LatLng, String) -> Unit) {
    val scope = rememberCoroutineScope()
    var pos by remember { mutableStateOf(start) }
    var time by remember { mutableStateOf("07:45") }
    var busy by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetGesturesEnabled = false) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Propose a pickup", style = MaterialTheme.typography.titleLarge)
            PlaceSearch(campus) { _, ll -> pos = ll }
            MapCard(center = pos ?: campus, zoom = if (pos != null) 16f else 12f, height = 240.dp, interactive = true, pins = listOfNotNull(pos?.let { Pin(it, "Pickup", HUE_PICK) }), onTap = { pos = it })
            TimeField("Pickup time", time, Modifier.fillMaxWidth()) { time = it }
            Button(enabled = pos != null && !busy, modifier = Modifier.fillMaxWidth().height(52.dp), onClick = { busy = true; scope.launch { onSend(pos!!, time); busy = false } }) { Text(if (busy) "Sending…" else "Send proposal") }
        }
    }
}
