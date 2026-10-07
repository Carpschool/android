package ca.carpschool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ca.carpschool.data.*
import ca.carpschool.ui.components.*
import ca.carpschool.ui.theme.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlin.math.roundToInt

fun campusOf(meta: SchoolMeta?): LatLng = meta?.campus?.coordinates?.takeIf { it.size == 2 }?.latLng() ?: LatLng(49.26, -123.25)

@Composable
fun HomesScreen(nav: NavController) {
    val toast = LocalToast.current
    val meta by Carp.meta.collectAsState()
    val q = rememberQuery("homes") { Carp.get<List<Home>>("/homes") }
    var editing by remember { mutableStateOf<Home?>(null) }
    var adding by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Home?>(null) }
    val max = meta?.limits?.homes ?: 3
    BackScreen("Homes", nav) {
        item { Text("Pickup points you leave from or return to. Riders walk up to the radius to meet a driver.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (adding || editing != null) item {
            HomeEditor(editing, campusOf(meta), onCancel = { adding = false; editing = null }) { adding = false; editing = null; q.refresh() }
        }
        when (val s = q.state) {
            is Load.Loading -> item { Skeleton() }
            is Load.Err -> item { ErrorBox(s.message, q.refresh) }
            is Load.Ok -> {
                itemsIndexed(s.value, key = { _, h -> h.id }) { i, h ->
                    Ticket(Modifier.rise(i), accent = Amber, stub = { Stub("WALK", "${h.walkingRadius}m") }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(h.label, style = MaterialTheme.typography.titleMedium)
                                Text("%.4f, %.4f".format(h.location.lat, h.location.lng), style = MaterialTheme.typography.bodySmall, fontFamily = Mono, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row { TextButton(onClick = { editing = h; adding = false }) { Text("Edit") }; TextButton(onClick = { del = h }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }
                    }
                }
                if (!adding && editing == null) item {
                    if (s.value.size < max) Button(onClick = { adding = true }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AddHome, null); Spacer(Modifier.width(8.dp)); Text("Add home (${s.value.size}/$max)") }
                    else InfoBox("You've reached $max homes. Delete one to add another.")
                }
            }
        }
    }
    del?.let { h -> ConfirmDialog("Delete ${h.label}?", "Requests or drives using this home may stop matching.", "Delete", danger = true, onDismiss = { del = null }) {
        try { Carp.api("/homes/${h.id}", "DELETE"); toast("Home deleted"); q.refresh() } catch (e: Exception) { toast(errText(e)) }; del = null } }
}

@Composable
fun HomeEditor(home: Home?, campus: LatLng, onCancel: () -> Unit, onSaved: () -> Unit) {
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var pos by remember(home) { mutableStateOf(home?.location?.latLng()) }
    var label by remember(home) { mutableStateOf(home?.label ?: "") }
    var r by remember(home) { mutableFloatStateOf((home?.walkingRadius ?: 80).toFloat()) }
    var busy by remember { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (home == null) "New home" else "Edit home", style = MaterialTheme.typography.titleMedium)
            PlaceSearch(campus) { name, ll -> pos = ll; if (label.isBlank()) label = name.take(40) }
            MapCard(center = pos ?: campus, zoom = if (pos != null) 16f else 12f, height = 260.dp, interactive = true,
                pins = listOfNotNull(pos?.let { Pin(it, label.ifBlank { "Pickup point" }) }), circle = pos?.let { it to r.toDouble() }, onTap = { pos = it })
            Text(if (pos == null) "Search or tap the map to place your pin" else "Tap the map to adjust", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(label, { label = it.take(40) }, Modifier.fillMaxWidth(), label = { Text("Label") }, placeholder = { Text("Mom's place") }, singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Walking radius", Modifier.weight(1f)); Text("${r.roundToInt()} m", fontFamily = Mono) }
            Slider(r, { r = (it / 10).roundToInt() * 10f }, valueRange = 10f..200f, steps = 18)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Button(enabled = !busy && pos != null && label.isNotBlank(), onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val body = Carp.enc(HomeBody(label.trim(), Point.of(pos!!.latitude, pos!!.longitude), r.roundToInt()))
                            if (home == null) Carp.api("/homes", "POST", body) else Carp.api("/homes/${home.id}", "PUT", body)
                            toast("Home saved"); onSaved()
                        } catch (e: Exception) { toast(errText(e)) }
                        busy = false
                    }
                }) { Text(if (busy) "Saving…" else "Save") }
            }
        }
    }
}

@Composable
fun NewCommuteScreen(me: Me, nav: NavController) {
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val meta by Carp.meta.collectAsState()
    val homes = rememberQuery("homes") { Carp.get<List<Home>>("/homes") }
    var homeId by remember { mutableStateOf<String?>(null) }
    var dir by remember { mutableStateOf("to-school") }
    var repeat by remember { mutableStateOf(true) }
    var days by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    var date by remember { mutableStateOf(todayISO()) }
    var start by remember { mutableStateOf("07:30") }
    var end by remember { mutableStateOf("08:15") }
    var seats by remember { mutableIntStateOf(3) }
    var route by remember { mutableStateOf<RouteResult?>(null) }
    var routeErr by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(homes.data) { if (homeId == null) homeId = homes.data?.firstOrNull()?.id }
    val home = homes.data?.find { it.id == homeId }
    val campus = campusOf(meta)
    LaunchedEffect(home, dir, me.isDriver, meta) {
        route = null; routeErr = null
        if (!me.isDriver || home == null || meta == null) return@LaunchedEffect
        val h = home.location.coordinates; val s = meta!!.campus!!.coordinates
        try { route = if (dir == "to-school") Geo.route(h, s) else Geo.route(s, h) } catch (e: Exception) { routeErr = errText(e) }
    }
    val maxSeats = minOf(meta?.limits?.seats ?: 4, 4)
    val timeOk = start <= end
    val ok = home != null && timeOk && (if (repeat) days.isNotEmpty() else true) && (!me.isDriver || route != null)
    BackScreen(if (me.isDriver) "Offer a drive" else "Request a ride", nav) {
        item {
            Kicker("Direction")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                listOf("to-school" to "To school", "home" to "Home").forEachIndexed { i, (k, l) ->
                    SegmentedButton(dir == k, { dir = k }, SegmentedButtonDefaults.itemShape(i, 2)) { Text(l) }
                }
            }
        }
        item {
            Kicker("Home")
            Loaded(homes, 1, 56) { list ->
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    list.forEach { h -> FilterChip(homeId == h.id, { homeId = h.id }, { Text(h.label) }, leadingIcon = { Icon(Icons.Outlined.Home, null, Modifier.size(18.dp)) }) }
                }
            }
        }
        item {
            Kicker("When")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp)) {
                SegmentedButton(repeat, { repeat = true }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Repeats weekly") }
                SegmentedButton(!repeat, { repeat = false }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("One day") }
            }
            if (repeat) Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                DAYS.forEachIndexed { i, d -> FilterChip(i in days, { days = if (i in days) days - i else days + i }, { Text(d.take(2)) }, Modifier.weight(1f)) }
            } else DateField(date) { date = it }
        }
        item {
            Kicker(if (dir == "to-school") "Pickup window" else "Leaving window")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) {
                TimeField("Earliest", start, Modifier.weight(1f)) { start = it }
                TimeField("Latest", end, Modifier.weight(1f), isError = !timeOk) { end = it }
            }
            if (!timeOk) Text("Latest must be after earliest", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (me.isDriver) {
            item {
                Kicker("Seats")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    (1..maxSeats).forEach { n -> FilterChip(seats == n, { seats = n }, { Text("$n") }) }
                }
            }
            item {
                Kicker("Route")
                Spacer(Modifier.height(6.dp))
                if (home != null) {
                    val line = route?.coordinates?.map { it.latLng() } ?: emptyList()
                    MapCard(center = home.location.latLng(), fit = true, height = 220.dp, line = line,
                        pins = listOf(Pin(if (dir == "to-school") home.location.latLng() else campus, "Start", HUE_START), Pin(if (dir == "to-school") campus else home.location.latLng(), "End", HUE_END)))
                    Spacer(Modifier.height(6.dp))
                    when {
                        routeErr != null -> ErrorBox(routeErr!!)
                        route == null -> Text("Finding the road route…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> Text("%.1f km · about %d min".format(route!!.meters / 1000, (route!!.seconds / 60).roundToInt()), style = MaterialTheme.typography.bodyMedium, fontFamily = Mono)
                    }
                }
            }
        }
        item {
            Button(enabled = ok && !busy, modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 4.dp), onClick = {
                busy = true
                scope.launch {
                    try {
                        val c = CommuteBody(homeId!!, dir, if (repeat) emptyList() else listOf(date), if (repeat) days.sorted() else emptyList(), start, end)
                        val res = if (me.isDriver) Carp.api("/drives", "POST", Carp.enc(DriveBody(c, route!!.coordinates, seats))) else Carp.api("/requests", "POST", Carp.enc(c))
                        val id = (res as? JsonObject)?.get("_id")?.jsonPrimitive?.content
                        toast(if (me.isDriver) "Drive posted" else "Request posted")
                        nav.popBackStack()
                        if (id != null) nav.navigate(if (me.isDriver) "drive/$id" else "request/$id")
                    } catch (e: Exception) { toast(errText(e)) }
                    busy = false
                }
            }) { Text(if (busy) "Posting…" else if (me.isDriver) "Post drive" else "Post request") }
        }
    }
}

@Composable
fun DriveScreen(id: String, nav: NavController) {
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val meta by Carp.meta.collectAsState()
    val d = rememberQuery("drive", id) { Carp.get<Commute>("/drives/$id") }
    val matches = rememberQuery("matches", id) { Carp.get<List<Match>>("/drives/$id/matches") }
    val homes = rememberQuery("homes") { Carp.get<List<Home>>("/homes") }
    var cancel by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf<String?>(null) }
    BackScreen("Drive", nav, actions = { IconButton(onClick = { cancel = true }) { Icon(Icons.Outlined.DeleteOutline, "Cancel drive") } }) {
        item {
            Loaded(d, 1, 300) { c ->
                val home = homes.data?.find { it.id == c.homeId }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Kicker(dirLabel(c.direction))
                    Text("${schedule(c)}", style = MaterialTheme.typography.headlineSmall)
                    Text("${c.startTime}–${c.endTime} · ${c.availableSeats} of ${c.seats} seats open", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val line = c.route?.coordinates?.map { it.latLng() } ?: emptyList()
                    if (line.isNotEmpty()) MapCard(center = line.first(), fit = true, line = line, height = 240.dp,
                        pins = listOf(Pin(line.first(), "Start", HUE_START), Pin(line.last(), "End", HUE_END)) + c.passengers.map { Pin(it.pickup.latLng(), "Pickup", HUE_PICK) })
                    if (c.passengers.isNotEmpty()) {
                        OutlinedButton(onClick = { nav.navigate("ride/${c.id}") }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Pin, null); Spacer(Modifier.width(8.dp)); Text("Open carpool (${c.passengers.size} rider${if (c.passengers.size > 1) "s" else ""})") }
                    }
                }
            }
        }
        item { SectionTitle("Riders on your way", Modifier.padding(top = 12.dp)) { IconButton(onClick = matches.refresh) { Icon(Icons.Outlined.Refresh, "Refresh") } } }
        when (val s = matches.state) {
            is Load.Loading -> item { Skeleton(2, 72) }
            is Load.Err -> item { ErrorBox(s.message, matches.refresh) }
            is Load.Ok -> {
                if (s.value.isEmpty()) item { EmptyState(Icons.Outlined.PersonSearch, "No riders yet", "We'll list riders whose walk to your route fits their radius and time.") }
                itemsIndexed(s.value, key = { _, m -> m.requestId }) { i, m ->
                    Ticket(Modifier.rise(i), accent = Amber, stub = { Stub("WALK", "${m.distanceMeters.roundToInt()}m") }) {
                        Text("Rider ${shortId(m.rider)}", style = MaterialTheme.typography.titleMedium)
                        Text("Wants ${m.startTime}–${m.endTime}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(enabled = starting == null, onClick = {
                                starting = m.requestId
                                scope.launch {
                                    try {
                                        val r = Carp.api("/drives/$id/negotiations", "POST", buildJsonObject { put("requestId", m.requestId) }) as JsonObject
                                        nav.navigate("chat/${r["_id"]!!.jsonPrimitive.content}")
                                    } catch (e: Exception) { toast(errText(e)) }
                                    starting = null
                                }
                            }, modifier = Modifier.padding(top = 8.dp)) { Text(if (starting == m.requestId) "Opening…" else "Message") }
                            Spacer(Modifier.weight(1f))
                            SafetyMenu(m.rider, "this rider") { matches.refresh() }
                        }
                    }
                }
            }
        }
    }
    if (cancel) ConfirmDialog("Cancel this drive?", "Matched riders get an email and their requests reopen.", "Cancel drive", danger = true, onDismiss = { cancel = false }) {
        try { Carp.api("/drives/$id", "DELETE"); toast("Drive cancelled"); nav.popBackStack() } catch (e: Exception) { toast(errText(e)) }; cancel = false
    }
}

@Composable
fun RequestScreen(id: String, nav: NavController) {
    val toast = LocalToast.current
    val r = rememberQuery("req", id) { Carp.get<Commute>("/requests/$id") }
    val homes = rememberQuery("homes") { Carp.get<List<Home>>("/homes") }
    val negs = rememberQuery("negs", id, pollMs = 15000) { Carp.get<List<Negotiation>>("/negotiations").filter { it.requestId == id } }
    var cancel by remember { mutableStateOf(false) }
    BackScreen("Request", nav, actions = { IconButton(onClick = { cancel = true }) { Icon(Icons.Outlined.DeleteOutline, "Cancel request") } }) {
        item {
            Loaded(r, 1, 260) { c ->
                val home = homes.data?.find { it.id == c.homeId }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Kicker(dirLabel(c.direction)); Spacer(Modifier.width(8.dp)); StatusChip(if (c.status == "locked") "Matched" else "Looking", if (c.status == "locked") "success" else "warn") }
                    Text(schedule(c), style = MaterialTheme.typography.headlineSmall)
                    Text("${c.startTime}–${c.endTime} from ${home?.label ?: "home"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (home != null) MapCard(center = home.location.latLng(), zoom = 16f, pins = listOf(Pin(home.location.latLng(), home.label)), circle = home.location.latLng() to home.walkingRadius.toDouble())
                }
            }
        }
        item { SectionTitle("Drivers who reached out", Modifier.padding(top = 12.dp)) }
        when (val s = negs.state) {
            is Load.Loading -> item { Skeleton(1, 72) }
            is Load.Err -> item { ErrorBox(s.message, negs.refresh) }
            is Load.Ok -> {
                if (s.value.isEmpty()) item { EmptyState(Icons.Outlined.HourglassEmpty, "Waiting for drivers", "When a driver passing near you messages, it shows up here and in Chats.") }
                itemsIndexed(s.value, key = { _, n -> n.id }) { i, n ->
                    Ticket(Modifier.rise(i), onClick = { nav.navigate("chat/${n.id}") }, accent = if (n.status == "locked") Moss else Amber) {
                        Text("Driver ${shortId(n.driver)}", style = MaterialTheme.typography.titleMedium)
                        Text(if (n.status == "locked") "Carpool confirmed" else "Open chat", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    if (cancel) ConfirmDialog("Cancel this request?", "Open chats for it will close.", "Cancel request", danger = true, onDismiss = { cancel = false }) {
        try { Carp.api("/requests/$id", "DELETE"); toast("Request cancelled"); nav.popBackStack() } catch (e: Exception) { toast(errText(e)) }; cancel = false
    }
}
