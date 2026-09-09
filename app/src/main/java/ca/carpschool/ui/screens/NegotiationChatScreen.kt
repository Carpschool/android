package ca.carpschool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NegotiationChatScreen(
    negotiationId: String,
    onNavigateBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var showLocationModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onNavigateBack) { Text("Back") }
            Button(onClick = { showLocationModal = true }) {
                Text("Suggest Pickup Point")
            }
        }

        Text("Carpool Negotiation", style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("// TODO: Connect to Socket.io negotiation room and render chat stream", style = MaterialTheme.typography.bodySmall)

                // Sample In-Chat Proposal Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📍 Proposed Pickup Spot", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1D4ED8))
                        Text("Corner of 10th & Main", style = MaterialTheme.typography.bodyMedium)
                        Text("Proposed Time: 08:30 AM", style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { /* TODO: Confirm */ }) { Text("Confirm") }
                            OutlinedButton(onClick = { /* TODO: Deny */ }) { Text("Deny") }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                // TODO: Send message over Socket.io
                messageText = ""
            }) {
                Text("Send")
            }
        }
    }
}
