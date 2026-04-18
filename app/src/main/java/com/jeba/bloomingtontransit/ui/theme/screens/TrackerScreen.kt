package com.jeba.bloomingtontransit.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.jeba.bloomingtontransit.ui.theme.ViewModel.TransitViewModel

@Composable
fun TrackerScreen(viewModel: TransitViewModel, modifier: Modifier = Modifier) {
    val selectedBus by viewModel.selectedBus.collectAsState()
    val allVehicles by viewModel.vehiclePositions.collectAsState()

    // Keep selected bus updated with latest position from live feed
    val liveBus = remember(selectedBus, allVehicles) {
        selectedBus?.let { selected ->
            allVehicles.find { it.vehicleId == selected.vehicleId } ?: selected
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        Text(
            text = "Bus Tracker",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        if (liveBus == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No bus selected",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap a bus on the Map screen to track it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val busPosition = LatLng(liveBus.lat, liveBus.lon)
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(busPosition, 15f)
            }

            // Auto-follow the bus
            LaunchedEffect(liveBus.lat, liveBus.lon) {
                cameraPositionState.position =
                    CameraPosition.fromLatLngZoom(busPosition, 15f)
            }

            // Map takes 60% of screen
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = busPosition),
                    title = "Route ${liveBus.routeId}",
                    snippet = "Bus ${liveBus.vehicleId}"
                )
            }

            // Info card takes 40% of screen
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Route ${liveBus.routeId}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    HorizontalDivider()
                    InfoRow(label = "Bus ID", value = liveBus.vehicleId)
                    InfoRow(label = "Latitude", value = String.format("%.5f", liveBus.lat))
                    InfoRow(label = "Longitude", value = String.format("%.5f", liveBus.lon))
                    InfoRow(
                        label = "Bearing",
                        value = "${liveBus.bearing.toInt()}°"
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}