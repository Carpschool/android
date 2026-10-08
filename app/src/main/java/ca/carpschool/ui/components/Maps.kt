package ca.carpschool.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ca.carpschool.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

data class Pin(val pos: LatLng, val title: String? = null, val hue: Float = BitmapDescriptorFactory.HUE_ORANGE)

const val HUE_START = 150f
const val HUE_END = 10f
const val HUE_PICK = 40f

@Composable
fun MapCard(
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    center: LatLng,
    zoom: Float = 15f,
    pins: List<Pin> = emptyList(),
    line: List<LatLng> = emptyList(),
    circle: Pair<LatLng, Double>? = null,
    fit: Boolean = false,
    interactive: Boolean = false,
    onTap: ((LatLng) -> Unit)? = null,
) {
    val cam = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(center, zoom) }
    var loaded by remember { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()
    val pts = remember(pins, line) { pins.map { it.pos } + line }
    LaunchedEffect(loaded, pts, center, fit) {
        if (!loaded) return@LaunchedEffect
        if (fit && pts.size > 1) {
            val b = LatLngBounds.builder().apply { pts.forEach { include(it) } }.build()
            runCatching { cam.animate(CameraUpdateFactory.newLatLngBounds(b, 90)) }
        } else cam.animate(CameraUpdateFactory.newLatLngZoom(center, zoom))
    }
    val accent = MaterialTheme.colorScheme.secondary
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(18.dp))) {
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cam,
            onMapLoaded = { loaded = true },
            onMapClick = { onTap?.invoke(it) },
            properties = MapProperties(mapStyleOptions = if (dark) MapStyleOptions(NIGHT_STYLE) else null),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false, mapToolbarEnabled = false, myLocationButtonEnabled = false,
                scrollGesturesEnabled = interactive, zoomGesturesEnabled = interactive, rotationGesturesEnabled = false, tiltGesturesEnabled = false,
            ),
        ) {
            if (line.size > 1) Polyline(points = line, color = if (dark) Color(0xFFBFCBEA) else Ink, width = 12f, jointType = JointType.ROUND, startCap = RoundCap(), endCap = RoundCap())
            circle?.let { (c, r) -> Circle(center = c, radius = r, fillColor = accent.copy(alpha = 0.18f), strokeColor = accent, strokeWidth = 4f) }
            pins.forEach { p -> Marker(state = rememberUpdatedMarkerState(position = p.pos), title = p.title, icon = BitmapDescriptorFactory.defaultMarker(p.hue)) }
        }
    }
}

private const val NIGHT_STYLE = """[
{"elementType":"geometry","stylers":[{"color":"#1b2236"}]},
{"elementType":"labels.text.fill","stylers":[{"color":"#9aa3b8"}]},
{"elementType":"labels.text.stroke","stylers":[{"color":"#111827"}]},
{"featureType":"road","elementType":"geometry","stylers":[{"color":"#2b3550"}]},
{"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#3a4566"}]},
{"featureType":"water","elementType":"geometry","stylers":[{"color":"#0c1322"}]},
{"featureType":"poi","stylers":[{"visibility":"off"}]},
{"featureType":"transit","stylers":[{"visibility":"off"}]}
]"""
