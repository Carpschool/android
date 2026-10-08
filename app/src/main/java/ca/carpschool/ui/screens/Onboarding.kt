package ca.carpschool.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.carpschool.data.*
import ca.carpschool.ui.components.*
import ca.carpschool.ui.theme.*
import com.clerk.ui.auth.AuthView
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@Composable
fun Wordmark(color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(Amber), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.DirectionsCar, null, tint = Ink, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text("carpschool", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, letterSpacing = (-0.5).sp, color = color)
    }
}

@Composable
fun LandingScreen() {
    var auth by remember { mutableStateOf(false) }
    AnimatedContent(auth, label = "landing") { showAuth ->
        if (showAuth) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { auth = false }) { Text("Back") }
                }
                AuthView(modifier = Modifier.fillMaxSize())
            }
        } else {
            Box(Modifier.fillMaxSize().background(Ink)) {
                DarkSystemBars()
                Column(Modifier.fillMaxSize().systemBarsPadding().padding(28.dp)) {
                    Wordmark(Paper)
                    RouteHero(Modifier.weight(1f).fillMaxWidth())
                    Text("TO SCHOOL · HOME · TOGETHER", style = Overline, color = Amber, modifier = Modifier.rise(0))
                    Spacer(Modifier.height(12.dp))
                    Text("Share the ride\nwith classmates\nwho live nearby.", style = MaterialTheme.typography.displaySmall, color = Paper, modifier = Modifier.rise(1))
                    Spacer(Modifier.height(16.dp))
                    Text("Verified students only. Pickups agreed in chat, confirmed with a boarding PIN. No live tracking.",
                        style = MaterialTheme.typography.bodyLarge, color = Paper.copy(alpha = 0.72f), modifier = Modifier.rise(2))
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { auth = true }, modifier = Modifier.fillMaxWidth().height(56.dp).rise(3),
                        colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Ink)) {
                        Text("Get started", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardFrame(step: Int, title: String, sub: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Wordmark(); Spacer(Modifier.weight(1f)); Kicker("Step $step of 3")
        }
        LinearProgressIndicator(progress = { step / 3f }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 28.dp).clip(CircleShape), color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh, drawStopIndicator = {})
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(sub, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
        content()
    }
}

@Composable
fun SchoolScreen(onSignOut: () -> Unit) {
    val schools by Carp.schools.collectAsState()
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { if (schools !is Load.Ok) Carp.loadSchools() }
    OnboardFrame(1, "Pick your school", "Only schools trusted by the Carpschool network are listed.") {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search schools") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true)
        Spacer(Modifier.height(16.dp))
        when (val s = schools) {
            is Load.Loading -> Skeleton(3, 72)
            is Load.Err -> ErrorBox(s.message) { scope.launch { Carp.loadSchools() } }
            is Load.Ok -> {
                val list = s.value.filter { q.isBlank() || it.name.contains(q, true) || it.schoolCode.contains(q, true) || it.domains.any { d -> d.contains(q, true) } }
                if (list.isEmpty()) EmptyState(Icons.Outlined.School, "No schools found", "Try a different name, or ask your school to join the network.")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    list.forEachIndexed { i, sc ->
                        Ticket(Modifier.rise(i), accent = Amber, onClick = { busy = sc.schoolCode; scope.launch { Carp.chooseSchool(sc); busy = null } }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(sc.name, style = MaterialTheme.typography.titleMedium)
                                    Text(sc.domains.joinToString(" · ") { "@$it" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (busy == sc.schoolCode) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                                else Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Sign out") }
    }
}

@Composable
fun VerifyScreen(school: School, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val domainOk = school.domains.isEmpty() || school.domains.any { email.trim().lowercase().endsWith("@" + it.lowercase()) }
    OnboardFrame(2, "Verify you're a student", "We'll email a 6 digit code to your school address. ${school.name} uses ${school.domains.joinToString(", ") { "@$it" }}.") {
        AnimatedContent(sent, label = "verify") { isSent ->
            Column {
                if (!isSent) {
                    OutlinedTextField(email, { email = it.trim(); err = null }, Modifier.fillMaxWidth(), label = { Text("School email") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Email, null) }, isError = email.contains("@") && !domainOk,
                        supportingText = { if (email.contains("@") && !domainOk) Text("Use your ${school.domains.firstOrNull()?.let { "@$it" } ?: "school"} address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    Spacer(Modifier.height(16.dp))
                    Button(enabled = !busy && email.contains("@") && domainOk, modifier = Modifier.fillMaxWidth().height(52.dp), onClick = {
                        busy = true; err = null
                        scope.launch { try { Carp.api("/edu/send", "POST", buildJsonObject { put("email", email) }); sent = true } catch (e: Exception) { err = errText(e) }; busy = false }
                    }) { Text(if (busy) "Sending…" else "Send code") }
                } else {
                    Text("Code sent to $email", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6); err = null }, Modifier.fillMaxWidth(), label = { Text("6 digit code") }, singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = Mono, letterSpacing = 8.sp, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    Spacer(Modifier.height(16.dp))
                    Button(enabled = !busy && code.length == 6, modifier = Modifier.fillMaxWidth().height(52.dp), onClick = {
                        busy = true; err = null
                        scope.launch { try { Carp.api("/edu/verify", "POST", buildJsonObject { put("code", code) }); Carp.refreshMe() } catch (e: Exception) { err = errText(e) }; busy = false }
                    }) { Text(if (busy) "Checking…" else "Verify") }
                    TextButton(onClick = { sent = false; code = "" }, modifier = Modifier.fillMaxWidth()) { Text("Use a different email") }
                }
            }
        }
        err?.let { Spacer(Modifier.height(12.dp)); ErrorBox(it) }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Change school") }
    }
}

@Composable
fun RoleScreen() {
    val scope = rememberCoroutineScope()
    var role by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf(com.clerk.api.Clerk.userFlow.value?.let { listOfNotNull(it.firstName, it.lastName).joinToString(" ") } ?: "") }
    var phone by remember { mutableStateOf("") }
    var personal by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var license by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val valid = role != null && name.isNotBlank() && phone.length >= 7 && (role == "rider" || (personal.contains("@") && make.isNotBlank() && color.isNotBlank() && plate.isNotBlank() && license))
    OnboardFrame(3, "How will you ride?", "This choice is permanent for your account at this school.") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(Triple("rider", "Rider", Icons.Outlined.Hail), Triple("driver", "Driver", Icons.Outlined.DirectionsCar)).forEach { (k, l, ic) ->
                val sel = role == k
                OutlinedCard(onClick = { role = k }, modifier = Modifier.weight(1f).height(120.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest, contentColor = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface),
                    border = CardDefaults.outlinedCardBorder(enabled = !sel)) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Icon(ic, null, tint = if (sel) Amber else MaterialTheme.colorScheme.onSurfaceVariant)
                        Column { Text(l, style = MaterialTheme.typography.titleLarge); Text(if (k == "rider") "Get picked up" else "Offer seats", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
        if (role != null) {
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Full name") }, singleLine = true, leadingIcon = { Icon(Icons.Outlined.Person, null) })
                PhoneField(phone) { phone = it }
                if (role == "driver") {
                    OutlinedTextField(personal, { personal = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Personal email") }, singleLine = true, leadingIcon = { Icon(Icons.Outlined.AlternateEmail, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    Kicker("Your car", Modifier.padding(top = 8.dp))
                    OutlinedTextField(make, { make = it.take(40) }, Modifier.fillMaxWidth(), label = { Text("Make and model") }, placeholder = { Text("Honda Civic") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(color, { color = it.take(20) }, Modifier.weight(1f), label = { Text("Color") }, singleLine = true)
                        OutlinedTextField(plate, { plate = it.uppercase().take(10) }, Modifier.weight(1f), label = { Text("Plate") }, singleLine = true, textStyle = LocalTextStyle.current.copy(fontFamily = Mono))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(license, { license = it })
                        Text("I have a valid driver's license and insurance", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            err?.let { Spacer(Modifier.height(12.dp)); ErrorBox(it) }
            Spacer(Modifier.height(20.dp))
            Button(enabled = valid && !busy, onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (busy) "Saving…" else "Continue as ${if (role == "driver") "driver" else "rider"}") }
        }
    }
    if (confirm) ConfirmDialog("Lock in ${role}?", "You can't switch roles later at this school.", "Lock it in", onDismiss = { confirm = false }) {
        busy = true; err = null
        val body = buildJsonObject {
            put("role", role!!); put("name", name.trim()); put("phone", phone.trim())
            if (role == "driver") {
                put("personalEmail", personal); put("licenseConfirmed", true)
                putJsonObject("car") { put("make", make.trim()); put("color", color.trim()); put("plate", plate.trim()) }
            }
        }
        try { Carp.api("/profile", "POST", body); Carp.refreshMe() } catch (e: Exception) { err = errText(e) }
        busy = false; confirm = false
    }
}


/** Forces light status/nav icons while this screen is shown (landing is always ink). */
@Composable
fun DarkSystemBars() {
    val view = androidx.compose.ui.platform.LocalView.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    DisposableEffect(Unit) {
        val w = (view.context as android.app.Activity).window
        val c = androidx.core.view.WindowCompat.getInsetsController(w, view)
        c.isAppearanceLightStatusBars = false; c.isAppearanceLightNavigationBars = false
        onDispose { c.isAppearanceLightStatusBars = !dark; c.isAppearanceLightNavigationBars = !dark }
    }
}

/** Quiet illustration: a dashed road from home to school with two stops. */
@Composable
fun RouteHero(modifier: Modifier) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "hero")
    val phase by t.animateFloat(0f, 40f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2400, easing = androidx.compose.animation.core.LinearEasing)), label = "dash")
    androidx.compose.foundation.Canvas(modifier.padding(vertical = 24.dp)) {
        val w = size.width; val h = size.height
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.08f, h * 0.82f)
            cubicTo(w * 0.45f, h * 0.95f, w * 0.15f, h * 0.35f, w * 0.55f, h * 0.42f)
            cubicTo(w * 0.85f, h * 0.48f, w * 0.7f, h * 0.12f, w * 0.9f, h * 0.15f)
        }
        drawPath(p, Paper.copy(alpha = 0.10f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 18.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawPath(p, Amber, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 12.dp.toPx()), -phase.dp.toPx())))
        drawCircle(Paper, 7.dp.toPx(), androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.82f))
        drawCircle(Amber, 5.dp.toPx(), androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.42f))
        drawCircle(Amber, 11.dp.toPx(), androidx.compose.ui.geometry.Offset(w * 0.9f, h * 0.15f))
        drawCircle(Ink, 5.dp.toPx(), androidx.compose.ui.geometry.Offset(w * 0.9f, h * 0.15f))
    }
}
