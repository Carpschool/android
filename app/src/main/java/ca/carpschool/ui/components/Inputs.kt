package ca.carpschool.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ca.carpschool.data.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Places autocomplete field. */
@Composable
fun PlaceSearch(bias: LatLng?, onPick: (String, LatLng) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Suggestion>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(q) {
        if (q.length < 3) { results = emptyList(); return@LaunchedEffect }
        delay(250)
        results = try { err = null; Geo.search(ctx, q, bias) } catch (e: Exception) { err = "Search isn't available right now. Tap the map to drop a pin."; emptyList() }
    }
    Column {
        OutlinedTextField(q, { q = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("Search address") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true)
        err?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
        if (results.isNotEmpty()) {
            ElevatedCard(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                results.take(5).forEach { s ->
                    ListItem(
                        headlineContent = { Text(s.main) },
                        supportingContent = { Text(s.secondary, maxLines = 1) },
                        leadingContent = { Icon(Icons.Outlined.Place, null) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.clickableNoRipple {
                            scope.launch {
                                runCatching { Geo.place(ctx, s.id) }.getOrNull()?.let { (name, ll) -> onPick(s.main.ifBlank { name }, ll); q = ""; results = emptyList() }
                            }
                        },
                    )
                }
            }
        }
    }
}

fun Modifier.clickableNoRipple(onClick: () -> Unit) = this.clickable(onClick = onClick)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(label: String, value: String, modifier: Modifier = Modifier, isError: Boolean = false, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier.heightIn(min = 56.dp), shape = MaterialTheme.shapes.small,
        colors = if (isError) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors()) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontFamily = ca.carpschool.ui.theme.Mono)
        }
    }
    if (open) {
        val (h, m) = value.split(":").map { it.toIntOrNull() ?: 0 }
        val st = rememberTimePickerState(h, m, is24Hour = false)
        AlertDialog(onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { onChange("%02d:%02d".format(st.hour, st.minute)); open = false }) { Text("Set") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
            title = { Text(label) },
            text = { TimePicker(st) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = MaterialTheme.shapes.small) {
        Icon(Icons.Outlined.CalendarMonth, null); Spacer(Modifier.width(8.dp))
        Text(runCatching { schedule(Commute(id = "", dates = listOf(value))) }.getOrDefault(value), Modifier.weight(1f))
    }
    if (open) {
        val today = LocalDate.now()
        val st = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
            },
        )
        DatePickerDialog(onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { st.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }; open = false }) { Text("Set") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }) { DatePicker(st) }
    }
}

/** Report / block overflow menu. */
@Composable
fun SafetyMenu(subject: String, who: String, onBlocked: (() -> Unit)? = null) {
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf(false) }
    var block by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    Box {
        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Safety options for $who") }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(text = { Text("Report $who") }, leadingIcon = { Icon(Icons.Outlined.Flag, null) }, onClick = { menu = false; report = true })
            DropdownMenuItem(text = { Text("Block $who") }, leadingIcon = { Icon(Icons.Outlined.Block, null) }, onClick = { menu = false; block = true })
        }
    }
    if (report) AlertDialog(onDismissRequest = { report = false }, title = { Text("Report $who") },
        text = { Column { Text("Your school admins will review this. The other person isn't told who reported them."); Spacer(Modifier.height(12.dp))
            OutlinedTextField(reason, { reason = it.take(1000) }, Modifier.fillMaxWidth(), label = { Text("What happened?") }, minLines = 3) } },
        confirmButton = { Button(enabled = reason.trim().length >= 3, onClick = { scope.toastingLaunch(toast) {
            Carp.api("/reports", "POST", kotlinx.serialization.json.buildJsonObject { put("subject", kotlinx.serialization.json.JsonPrimitive(subject)); put("reason", kotlinx.serialization.json.JsonPrimitive(reason.trim())) })
            toast("Report sent"); report = false; reason = "" } }) { Text("Send report") } },
        dismissButton = { TextButton(onClick = { report = false }) { Text("Cancel") } })
    if (block) ConfirmDialog("Block $who?", "You won't be matched or able to chat with each other.", "Block", danger = true, onDismiss = { block = false }) {
        try { Carp.api("/blocks", "POST", kotlinx.serialization.json.buildJsonObject { put("subject", kotlinx.serialization.json.JsonPrimitive(subject)) }); toast("Blocked"); onBlocked?.invoke() } catch (e: Exception) { toast(errText(e)) }
        block = false
    }
}

@Composable
fun PhoneField(value: String, onChange: (String) -> Unit) = OutlinedTextField(value, { onChange(it.take(30)) }, Modifier.fillMaxWidth(), label = { Text("Phone") },
    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), leadingIcon = { Icon(Icons.Outlined.Phone, null) })
