package ca.carpschool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ca.carpschool.data.*
import ca.carpschool.ui.components.*
import ca.carpschool.ui.theme.*
import coil3.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

@Composable
fun Screen(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) =
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackScreen(title: String, nav: NavController, actions: @Composable RowScope.() -> Unit = {}, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }, actions = actions)
    }) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(top = p.calculateTopPadding()).imePadding(), contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun Avatar(url: String?, name: String?, size: Int = 40) {
    if (!url.isNullOrBlank()) AsyncImage(url, name, Modifier.size(size.dp).clip(CircleShape))
    else Surface(Modifier.size(size.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Box(contentAlignment = Alignment.Center) { Text((name ?: "?").take(1).uppercase(), style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
fun CommuteTicket(c: Commute, homes: List<Home>, driver: Boolean, i: Int, onClick: () -> Unit) {
    val home = homes.find { it.id == c.homeId }
    val today = runsToday(c)
    Ticket(Modifier.rise(i), accent = if (c.status == "locked" || c.passengers.isNotEmpty()) Moss else if (today) Amber else MaterialTheme.colorScheme.primary, onClick = onClick,
        stub = { Stub(if (driver) "SEATS" else "WINDOW", if (driver) "${c.availableSeats}/${c.seats}" else c.startTime) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Kicker(dirLabel(c.direction)); Spacer(Modifier.width(8.dp))
            if (today) StatusChip("Today", "warn")
            if (c.status == "locked") StatusChip("Matched", "success")
        }
        Text(if (c.direction == "to-school") "${home?.label ?: "Home"} to school" else "School to ${home?.label ?: "home"}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
        Text("${schedule(c)} · ${c.startTime}–${c.endTime}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HomeScreen(me: Me, nav: NavController) {
    val homes = rememberQuery("homes") { Carp.get<List<Home>>("/homes") }
    val list = rememberQuery("commutes", me.role) { if (me.isDriver) Carp.get<List<Commute>>("/drives") else Carp.get<List<Commute>>("/requests") }
    val meta by Carp.meta.collectAsState()
    Screen {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Kicker(meta?.name ?: Carp.school?.name ?: "")
                    Text("${greeting()}, ${me.name?.substringBefore(" ") ?: "there"}", style = MaterialTheme.typography.headlineMedium)
                }
                Avatar(me.avatar, me.name, 44)
            }
        }
        val h = homes.data
        if (h != null && h.isEmpty()) item {
            EmptyState(Icons.Outlined.AddHome, "Add your home first", "Drop a pin where you can be picked up. Only matched carpoolers ever see it.") {
                Button(onClick = { nav.navigate("homes") }) { Text("Add home") }
            }
        }
        item {
            ElevatedCard(onClick = { if (h.isNullOrEmpty()) nav.navigate("homes") else nav.navigate("new") }, colors = CardDefaults.elevatedCardColors(containerColor = Ink, contentColor = Paper), modifier = Modifier.fillMaxWidth().rise(0)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (me.isDriver) "Offer a drive" else "Request a ride", style = MaterialTheme.typography.titleLarge)
                        Text(if (me.isDriver) "Share your route and empty seats" else "Find a classmate heading your way", style = MaterialTheme.typography.bodyMedium, color = Paper.copy(alpha = .7f))
                    }
                    FilledIconButton(onClick = { if (h.isNullOrEmpty()) nav.navigate("homes") else nav.navigate("new") }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Amber, contentColor = Ink)) { Icon(Icons.Outlined.Add, null) }
                }
            }
        }
        item { SectionTitle(if (me.isDriver) "Your drives" else "Your requests", Modifier.padding(top = 12.dp)) }
        when (val s = list.state) {
            is Load.Loading -> item { Skeleton() }
            is Load.Err -> item { ErrorBox(s.message, list.refresh) }
            is Load.Ok -> {
                val active = s.value.filter { it.status != "cancelled" }
                if (active.isEmpty()) item { EmptyState(Icons.Outlined.Route, "Nothing scheduled", if (me.isDriver) "Post a drive and riders along your route can find you." else "Post a request and drivers passing by will reach out.") }
                itemsIndexed(active, key = { _, c -> c.id }) { i, c -> CommuteTicket(c, h ?: emptyList(), me.isDriver, i) { nav.navigate(if (me.isDriver) "drive/${c.id}" else "request/${c.id}") } }
            }
        }
        item {
            TextButton(onClick = { nav.navigate("homes") }, Modifier.padding(top = 8.dp)) { Icon(Icons.Outlined.Home, null); Spacer(Modifier.width(8.dp)); Text("Manage homes (${h?.size ?: 0}/${meta?.limits?.homes ?: 3})") }
        }
    }
}
