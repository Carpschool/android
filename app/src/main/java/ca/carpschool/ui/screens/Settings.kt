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
import com.clerk.api.Clerk
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable data class Block(val subject: String, val createdAt: String = "")

@Composable
fun SettingsScreen(me: Me, nav: NavController) {
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val user by Clerk.userFlow.collectAsState()
    val blocks = rememberQuery("blocks") { runCatching { Carp.get<List<Block>>("/blocks") }.getOrElse { emptyList() } }
    var signOut by remember { mutableStateOf(false) }
    Screen {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Avatar(me.avatar, me.name, 84)
                Text(me.name ?: "", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
                Text(me.eduEmail ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(if (me.isDriver) "Driver" else "Rider", "warn"); StatusChip("Verified", "success")
                }
            }
        }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                ListItem(headlineContent = { Text("School") }, supportingContent = { Text(Carp.school?.name ?: "") }, leadingContent = { Icon(Icons.Outlined.School, null) })
                ListItem(headlineContent = { Text("Phone") }, supportingContent = { Text(me.phone ?: "Not set") }, leadingContent = { Icon(Icons.Outlined.Phone, null) })
                if (me.isDriver && me.car != null) ListItem(headlineContent = { Text("Car") }, supportingContent = { Text("${me.car.color} ${me.car.make} · ${me.car.plate}") }, leadingContent = { Icon(Icons.Outlined.DirectionsCar, null) })
                ListItem(headlineContent = { Text("Homes") }, leadingContent = { Icon(Icons.Outlined.Home, null) }, trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
                    modifier = Modifier.clickableNoRipple { nav.navigate("homes") })
            }
        }
        item { SectionTitle("Blocked", Modifier.padding(top = 8.dp)) }
        val b = blocks.data ?: emptyList()
        if (b.isEmpty()) item { Text("You haven't blocked anyone.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        itemsIndexed(b, key = { _, x -> x.subject }) { _, x ->
            ListItem(headlineContent = { Text("User ${shortId(x.subject)}") }, trailingContent = { TextButton(onClick = { scope.toastingLaunch(toast) { Carp.api("/blocks/${x.subject}", "DELETE"); blocks.refresh() } }) { Text("Unblock") } })
        }
        item {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { scope.launch { Carp.chooseSchool(null) } }, Modifier.fillMaxWidth()) { Text("Switch school") }
                Button(onClick = { signOut = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) { Text("Sign out") }
            }
        }
        item { Text("Carpschool never tracks you live. Boarding and drop-off each record one location snapshot.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
    }
    if (signOut) ConfirmDialog("Sign out?", "You'll need to sign in again to see your rides.", "Sign out", onDismiss = { signOut = false }) { Clerk.auth.signOut(); Carp.signedOut(); signOut = false }
}

