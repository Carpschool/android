package ca.carpschool.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SchoolSelectScreen(
    onSchoolSelected: (schoolCode: String, baseUrl: String, isTrusted: Boolean) -> Unit
) {
    var customUrl by remember { mutableStateOf("") }
    var showUntrustedWarning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Select Your School", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Verified Directory", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // Sample Verified School
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSchoolSelected("ubc", "https://ubc.carp.school", true)
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("University of British Columbia", style = MaterialTheme.typography.bodyLarge)
                        Text("https://ubc.carp.school", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Custom / Self-Hosted School", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = customUrl,
                    onValueChange = {
                        customUrl = it
                        showUntrustedWarning = it.isNotBlank()
                    },
                    label = { Text("https://rides.myschool.org") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showUntrustedWarning) {
                    Text(
                        "⚠️ Untrusted Server: This school server is self-hosted and has not been verified by Carpschool.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD97706)
                    )
                }

                Button(
                    onClick = {
                        onSchoolSelected("custom", customUrl.trim(), false)
                    },
                    enabled = customUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect to Custom Server")
                }
            }
        }
    }
}
