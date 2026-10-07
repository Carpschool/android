package ca.carpschool.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import ca.carpschool.data.*
import ca.carpschool.ui.components.*
import ca.carpschool.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@Composable
fun RidesScreen(me: Me, nav: NavController) {
    val q = rememberQuery("carpools", pollMs = 30000) { Carp.get<List<Commute>>("/carpools") }
    val homes = rememberQuery("homes") { Carp.get<List<Home>>("/homes") }
    Screen {
        item { PageHead("Confirmed", "Rides", "Locked carpools. Boarding happens with a 4 digit PIN.") }
        when (val s = q.state) {
            is Load.Loading -> item { Skeleton() }
            is Load.Err -> item { ErrorBox(s.message, q.refresh) }
            is Load.Ok -> {
                val (today, later) = s.value.filter { it.status != "cancelled" }.partition { runsToday(it) }
                if (s.value.isEmpty()) item { EmptyState(Icons.Outlined.DirectionsCar, "No confirmed rides", "Accept a pickup proposal in Chats and it'll show up here.") }
                if (today.isNotEmpty()) item { Kicker("Today", color = MaterialTheme.colorScheme.secondary) }
                itemsIndexed(today, key = { _, c -> "t" + c.id }) { i, c -> RideTicket(c, me, i) { nav.navigate("ride/${c.id}") } }
                if (later.isNotEmpty()) item { Kicker("Upcoming", Modifier.padding(top = 8.dp)) }
                itemsIndexed(later, key = { _, c -> "l" + c.id }) { i, c -> RideTicket(c, me, i + today.size) { nav.navigate("ride/${c.id}") } }
            }
        }
    }
}

@Composable
private fun RideTicket(c: Commute, me: Me, i: Int, onClick: () -> Unit) {
    val mine = c.passengers.firstOrNull()
    Ticket(Modifier.rise(i), accent = Moss, onClick = onClick, stub = { Stub("PICKUP", if (me.isDriver) c.startTime else mine?.time ?: c.startTime) }) {
        Kicker(dirLabel(c.direction))
        Text(if (me.isDriver) "${c.passengers.size} rider${if (c.passengers.size != 1) "s" else ""}" else "Driver ${shortId(c.owner)}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 2.dp))
        Text(schedule(c), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RideScreen(id: String, me: Me, nav: NavController) {
    val ctx = LocalContext.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val q = rememberQuery("carpool", id, pollMs = 20000) { Carp.get<Commute>("/carpools/$id") }
    var pin by remember { mutableStateOf<String?>(null) }
    var board by remember { mutableStateOf<Passenger?>(null) }
    var leave by remember { mutableStateOf<String?>(null) }
    var askedPerm by remember { mutableStateOf(false) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { askedPerm = true }

    BackScreen("Ride", nav) {
        item {
            Loaded(q, 1, 300) { c ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Kicker(dirLabel(c.direction)); Text(schedule(c), style = MaterialTheme.typography.headlineSmall)
                    Text("Window ${c.startTime}–${c.endTime}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val line = c.route?.coordinates?.map { it.latLng() } ?: emptyList()
                    val pins = c.passengers.map { Pin(it.pickup.latLng(), "Pickup ${it.time}", HUE_PICK) }
                    if (line.isNotEmpty() || pins.isNotEmpty()) MapCard(center = (pins.firstOrNull()?.pos ?: line.first()), zoom = 15f, fit = line.isNotEmpty(), line = line, pins = pins, height = 230.dp)
                    if (!Geo.hasLocation(ctx) && me.isDriver) InfoBox("Boarding and drop-off record one location snapshot each. Allow location for accuracy.") {
                        TextButton(onClick = { perm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Allow") }
                    }
                }
            }
        }
        val c = q.data
        if (c != null && !me.isDriver) {
            val mine = c.passengers.firstOrNull()
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Kicker("Boarding PIN")
                        Text(pin ?: "••••", fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 48.sp, letterSpacing = 10.sp, modifier = Modifier.padding(vertical = 8.dp))
                        Text(if (pin == null) "Show this to your driver when you get in. We don't store it, so get a fresh one when you need it." else "Tell your driver this code at pickup.",
                            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (mine?.status != "boarded" && mine?.status != "dropped") Button(onClick = { scope.toastingLaunch(toast) {
                            pin = (Carp.api("/carpools/$id/pin", "POST") as JsonObject)["pin"]!!.jsonPrimitive.content } }, Modifier.padding(top = 12.dp)) { Text(if (pin == null) "Show PIN" else "New PIN") }
                    }
                }
            }
            if (mine != null) item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(mine.status.ifBlank { "matched" }.replaceFirstChar { it.uppercase() }, if (mine.status == "boarded") "success" else "warn")
                    Text("  Pickup at ${mine.time}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f)); SafetyMenu(c.owner, "driver")
                }
            }
            if (mine != null && mine.status != "boarded" && mine.status != "dropped") item {
                OutlinedButton(onClick = { leave = mine.rider }, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Leave carpool") }
            }
        }
        if (c != null && me.isDriver) {
            item { SectionTitle("Riders", Modifier.padding(top = 8.dp)) }
            if (c.passengers.isEmpty()) item { EmptyState(Icons.Outlined.Group, "No riders", "Riders appear once a pickup proposal is accepted.") }
            itemsIndexed(c.passengers, key = { _, p -> p.rider }) { i, p ->
                Ticket(Modifier.rise(i), accent = if (p.status == "boarded") Moss else Amber, stub = { Stub("AT", p.time) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Rider ${shortId(p.rider)}", style = MaterialTheme.typography.titleMedium)
                            StatusChip(p.status.ifBlank { "waiting" }.replaceFirstChar { it.uppercase() }, if (p.status == "boarded") "success" else if (p.status == "dropped") "default" else "warn")
                        }
                        SafetyMenu(p.rider, "rider")
                    }
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (p.status) {
                            "boarded" -> Button(onClick = { scope.toastingLaunch(toast) {
                                val (loc, approx) = Geo.snapshot(ctx, p.pickup)
                                Carp.api("/carpools/$id/dropoff", "POST", buildJsonObject { put("rider", p.rider); put("location", Carp.enc(loc)) })
                                toast(if (approx) "Dropped off (approximate location)" else "Dropped off"); q.refresh() } }) { Text("Drop off") }
                            "dropped" -> Text("Trip complete", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> {
                                Button(onClick = { board = p }) { Icon(Icons.Outlined.Pin, null); Spacer(Modifier.width(6.dp)); Text("Board") }
                                TextButton(onClick = { leave = p.rider }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    board?.let { p -> BoardDialog(onDismiss = { board = null }) { code ->
        try {
            val (loc, approx) = Geo.snapshot(ctx, p.pickup)
            Carp.api("/carpools/$id/board", "POST", buildJsonObject { put("rider", p.rider); put("pin", code); put("location", Carp.enc(loc)) })
            toast(if (approx) "Boarded (approximate location)" else "Boarded"); board = null; q.refresh(); null
        } catch (e: Exception) { errText(e) }
    } }
    leave?.let { r -> ConfirmDialog(if (me.isDriver) "Remove this rider?" else "Leave this carpool?", "The seat reopens and both of you get an email.", if (me.isDriver) "Remove" else "Leave", danger = true, onDismiss = { leave = null }) {
        try { Carp.api("/carpools/$id/leave", "POST", buildJsonObject { put("rider", r) }); toast("Done"); if (me.isDriver) q.refresh() else nav.popBackStack() } catch (e: Exception) { toast(errText(e)) }; leave = null } }
}

@Composable
private fun BoardDialog(onDismiss: () -> Unit, onSubmit: suspend (String) -> String?) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Enter rider's PIN") },
        text = { Column {
            Text("Ask the rider for the 4 digit code on their phone.")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(code, { code = it.filter(Char::isDigit).take(4); err = null }, Modifier.fillMaxWidth(), singleLine = true, isError = err != null,
                supportingText = { err?.let { Text(it) } },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontFamily = Mono, letterSpacing = 12.sp, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        } },
        confirmButton = { Button(enabled = code.length == 4 && !busy, onClick = { busy = true; scope.launch { err = onSubmit(code); busy = false } }) { Text(if (busy) "Checking…" else "Board") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } })
}
