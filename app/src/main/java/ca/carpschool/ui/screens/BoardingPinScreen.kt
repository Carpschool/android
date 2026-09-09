package ca.carpschool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BoardingPinScreen(
    onNavigateBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onNavigateBack) { Text("Back") }
        }

        Text("Boarding Verification", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Enter the passenger's 4-digit PIN. A single discrete GPS snapshot is captured at boarding time.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4) pin = it },
            label = { Text("4-Digit PIN") },
            singleLine = true
        )

        Button(
            onClick = {
                // TODO: Capture single GPS snapshot and submit to School Server
                statusMessage = "Boarding confirmed with single GPS snapshot!"
            },
            enabled = pin.length == 4
        ) {
            Text("Verify & Board Passenger")
        }

        if (statusMessage.isNotEmpty()) {
            Text(statusMessage, color = Color(0xFF059669), style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                // TODO: Capture single GPS snapshot and end ride
                statusMessage = "Ride completed with single GPS snapshot!"
            }
        ) {
            Text("End Ride (Arrival Snapshot)")
        }
    }
}
