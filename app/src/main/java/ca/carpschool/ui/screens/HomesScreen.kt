package ca.carpschool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomesScreen(
    onNavigateToCorridor: () -> Unit,
    onNavigateToBoarding: () -> Unit
) {
    var label by remember { mutableStateOf("Primary Home") }
    var address by remember { mutableStateOf("1234 Student Blvd, Vancouver, BC") }
    var walkingRadius by remember { mutableStateOf(75f) } // 10m to 200m

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Saved Homes & Walking Radius", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add New Location", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. Home, Dorm)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Street Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Walking Radius:")
                    Text("${walkingRadius.toInt()} meters", style = MaterialTheme.typography.bodyLarge)
                }

                Slider(
                    value = walkingRadius,
                    onValueChange = { walkingRadius = it },
                    valueRange = 10f..200f,
                    steps = 19
                )

                Button(
                    onClick = {
                        // TODO: Call NetworkService.saveHome(...)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Location")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNavigateToCorridor, modifier = Modifier.weight(1f)) {
                Text("Driver Corridor")
            }
            Button(onClick = onNavigateToBoarding, modifier = Modifier.weight(1f)) {
                Text("Boarding PIN")
            }
        }
    }
}
