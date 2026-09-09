package ca.carpschool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ca.carpschool.ui.screens.*
import ca.carpschool.ui.theme.CarpschoolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CarpschoolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var selectedSchoolCode by remember { mutableStateOf("") }
                    var schoolBaseUrl by remember { mutableStateOf("http://10.0.2.2:5000") }

                    NavHost(navController = navController, startDestination = "school_select") {
                        composable("school_select") {
                            SchoolSelectScreen(
                                onSchoolSelected = { code, url, isTrusted ->
                                    selectedSchoolCode = code
                                    schoolBaseUrl = url
                                    navController.navigate("homes")
                                }
                            )
                        }
                        composable("homes") {
                            HomesScreen(
                                onNavigateToCorridor = { navController.navigate("corridor") },
                                onNavigateToBoarding = { navController.navigate("boarding") }
                            )
                        }
                        composable("corridor") {
                            CorridorMatchingScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onStartNegotiation = { appId -> navController.navigate("chat/$appId") }
                            )
                        }
                        composable("chat/{id}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id") ?: ""
                            NegotiationChatScreen(
                                negotiationId = id,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("boarding") {
                            BoardingPinScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
