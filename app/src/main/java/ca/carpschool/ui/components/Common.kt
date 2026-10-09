package ca.carpschool.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.carpschool.data.Load
import ca.carpschool.data.errText
import ca.carpschool.ui.theme.Mono
import ca.carpschool.ui.theme.Overline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Simple loader: re-runs [block] when [key] or refresh() changes. Optional polling. */
class Query<T>(val state: Load<T>, val refresh: () -> Unit) {
    val data: T? get() = (state as? Load.Ok)?.value
}

@Composable
fun <T> rememberQuery(vararg keys: Any?, pollMs: Long = 0, enabled: Boolean = true, block: suspend () -> T): Query<T> {
    var state by remember(*keys) { mutableStateOf<Load<T>>(Load.Loading) }
    var n by remember(*keys) { mutableIntStateOf(0) }
    LaunchedEffect(*keys, n, enabled) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            state = try { Load.Ok(block()) } catch (e: Exception) { if (state is Load.Ok && n == 0) state else Load.Err(errText(e)) }
            if (pollMs <= 0) break
            delay(pollMs)
        }
    }
    return Query(state) { n++ }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) =
    Text(text.uppercase(), style = Overline, color = color, modifier = modifier)

@Composable
fun PageHead(kicker: String?, title: String, sub: String? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            if (kicker != null) Kicker(kicker, Modifier.padding(bottom = 6.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
        action?.invoke()
    }
}

/** Quiet, grouped route surface; the accent is a status marker, not decoration. */
@Composable
fun Ticket(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    stub: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val inner: @Composable () -> Unit = {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.padding(start = 20.dp, top = 24.dp).size(6.dp).background(accent, CircleShape))
            Column(Modifier.weight(1f).padding(start = 12.dp, top = 20.dp, end = 16.dp, bottom = 20.dp), content = content)
            if (stub != null) {
                VerticalDivider(Modifier.padding(vertical = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.width(92.dp).fillMaxHeight().padding(vertical = 14.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = stub)
            }
        }
    }
    val colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    if (onClick != null) OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)), colors = colors) { inner() }
    else OutlinedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)), colors = colors) { inner() }
}

@Composable
fun Stub(top: String, big: String) {
    Text(top, style = Overline, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(big, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, action: @Composable (() -> Unit)? = null) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            if (action != null) { Spacer(Modifier.height(16.dp)); action() }
        }
    }
}

@Composable
fun ErrorBox(message: String, retry: (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ErrorOutline, null)
            Text(message, Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
            if (retry != null) TextButton(onClick = retry) { Text("Retry") }
        }
    }
}

@Composable
fun InfoBox(message: String, container: Color = MaterialTheme.colorScheme.secondaryContainer, content: Color = MaterialTheme.colorScheme.onSecondaryContainer, action: @Composable (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = container, contentColor = content)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f).padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
            action?.invoke()
        }
    }
}

@Composable
fun Skeleton(rows: Int = 2, height: Int = 84) {
    // Static placeholder avoids constant peripheral motion and respects reduced motion.
    val a = 0.65f
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rows) { Box(Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(20.dp)).alpha(a).background(MaterialTheme.colorScheme.surfaceContainerHigh)) }
    }
}

@Composable
fun <T> Loaded(q: Query<T>, rows: Int = 2, height: Int = 84, content: @Composable (T) -> Unit) {
    when (val s = q.state) {
        is Load.Loading -> Skeleton(rows, height)
        is Load.Err -> ErrorBox(s.message, q.refresh)
        is Load.Ok -> content(s.value)
    }
}

/** Fade + rise enter animation, staggered by [index]. */
@Composable
fun Modifier.rise(index: Int = 0): Modifier {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val p by animateFloatAsState(if (shown) 1f else 0f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow), label = "rise")
    return this.graphicsLayer { alpha = p; // Opacity-only feedback keeps large content spatially stable.
        translationY = 0f }
}

@Composable
fun ConfirmDialog(
    title: String, body: String, confirm: String, danger: Boolean = false,
    onDismiss: () -> Unit, onConfirm: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) }, text = { Text(body) },
        confirmButton = {
            Button(enabled = !busy, onClick = { busy = true; scope.launch { onConfirm(); busy = false } },
                colors = if (danger) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError) else ButtonDefaults.buttonColors()) {
                Text(if (busy) "Working…" else confirm)
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Keep") } },
    )
}

@Composable
fun StatusChip(label: String, tone: String = "default") {
    val cs = MaterialTheme.colorScheme
    val (bg, fg) = when (tone) {
        "success" -> cs.tertiaryContainer to cs.onTertiaryContainer
        "warn" -> cs.secondaryContainer to cs.onSecondaryContainer
        "error" -> cs.errorContainer to cs.onErrorContainer
        else -> cs.surfaceContainerHigh to cs.onSurfaceVariant
    }
    Surface(color = bg, contentColor = fg, shape = CircleShape) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

/** Snackbar host shared through CompositionLocal. */
val LocalToast = staticCompositionLocalOf<(String) -> Unit> { {} }

fun CoroutineScope.toastingLaunch(toast: (String) -> Unit, block: suspend () -> Unit) = launch {
    try { block() } catch (e: Exception) { toast(errText(e)) }
}
