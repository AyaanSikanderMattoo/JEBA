package com.jeba.bloomingtontransit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeba.bloomingtontransit.data.db.StaticGtfsLoader
import com.jeba.bloomingtontransit.data.db.TransitDatabase
import com.jeba.bloomingtontransit.data.repository.RepositoryProvider
import com.jeba.bloomingtontransit.ui.theme.screens.MapScreen
import com.jeba.bloomingtontransit.ui.theme.screens.ScheduleScreen
import com.jeba.bloomingtontransit.ui.theme.screens.TrackerScreen
import com.jeba.bloomingtontransit.ui.theme.BloomingtonTransitTheme
import com.jeba.bloomingtontransit.ui.theme.ViewModel.TransitViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start DB load
        val db = TransitDatabase.getInstance(this)
        val loader = StaticGtfsLoader(this)
        lifecycleScope.launch(Dispatchers.IO) {
            loader.loadIfNeeded(db)
        }

        // Start polling
        val pollingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        RepositoryProvider.transitRepository.startPolling(pollingScope)

        enableEdgeToEdge()
        setContent {
            BloomingtonTransitTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val viewModel: TransitViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.LocationOn, contentDescription = "Map") },
                    label = { Text("Map") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = "Schedule") },
                    label = { Text("Schedule") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = "Tracker") },
                    label = { Text("Tracker") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> MapScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            1 -> ScheduleScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            2 -> TrackerScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}