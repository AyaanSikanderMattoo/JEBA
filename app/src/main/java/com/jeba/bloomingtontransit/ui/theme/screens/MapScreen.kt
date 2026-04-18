package com.jeba.bloomingtontransit.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.jeba.bloomingtontransit.ui.theme.ViewModel.TransitViewModel


@Composable
fun MapScreen(viewModel: TransitViewModel, modifier: Modifier = Modifier) {
    val vehicles by viewModel.vehiclePositions.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val selectedBus by viewModel.selectedBus.collectAsState()

    // Center on Bloomington, Indiana
    val bloomington = LatLng(39.1653, -86.5264)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bloomington, 13f)
    }

    LaunchedEffect(Unit) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(bloomington, 13f)
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            vehicles.forEach { bus ->
                val position = LatLng(bus.lat, bus.lon)
                Marker(
                    state = MarkerState(position = position),
                    title = "Route ${bus.routeId}",
                    snippet = "Bus ${bus.vehicleId}",
                    onClick = {
                        viewModel.selectBus(bus)
                        false
                    }
                )
            }
        }

        // Last updated indicator
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = if (vehicles.isEmpty()) "Loading buses..."
                else "${vehicles.size} buses • Updated ${viewModel.getLastUpdatedText()}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }

        // Selected bus info card at bottom
        selectedBus?.let { bus ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Route ${bus.routeId}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Bus ID: ${bus.vehicleId}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Location: ${String.format("%.4f", bus.lat)}, ${String.format("%.4f", bus.lon)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}