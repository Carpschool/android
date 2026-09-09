package ca.carpschool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CorridorMatchingScreen(
    onNavigateBack: () -> Unit,
    onStartNegotiation: (applicationId: String) -> Unit
) {
    var isMorning by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onNavigateBack) { Text("Back") }
            Text("Driver Corridor", style = MaterialTheme.typography.titleLarge)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = isMorning,
                onClick = { isMorning = true },
                label = { Text("Home → School") }
            )
            FilterChip(
                selected = !isMorning,
                onClick = { isMorning = false },
                label = { Text("School → Home") }
            )
        }

        Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("// TODO: Google Maps Compose showing commute corridor and rider walking circles")
            }
        }

        Text("Eligible Rider Applications", style = MaterialTheme.typography.titleMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Jane Doe (UBC Student)", style = MaterialTheme.typography.bodyLarge)
                Text("Requested Pickup: 08:30 AM", style = MaterialTheme.typography.bodySmall)
                Text("Walking radius: 75m", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onStartNegotiation("sample_app_id") }) {
                    Text("Reach Out to Rider")
                }
            }
        }
    }
}
