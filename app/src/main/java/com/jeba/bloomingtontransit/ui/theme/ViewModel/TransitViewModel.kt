package com.jeba.bloomingtontransit.ui.theme.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeba.bloomingtontransit.data.model.VehiclePosition
import com.jeba.bloomingtontransit.data.model.StopArrival
import com.jeba.bloomingtontransit.data.repository.ArrivalResult
import com.jeba.bloomingtontransit.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransitViewModel : ViewModel() {

    private val repository = RepositoryProvider.transitRepository

    // All live bus positions from repository
    val vehiclePositions: StateFlow<List<VehiclePosition>> = repository.vehiclePositions

    // All stop arrivals from repository
    val stopArrivals: StateFlow<List<StopArrival>> = repository.stopArrivals

    // Loading state
    val isLoading: StateFlow<Boolean> = repository.isLoading

    // Last updated timestamp
    val lastUpdated: StateFlow<Long> = repository.lastUpdated

    // Currently selected bus for tracker screen
    private val _selectedBus = MutableStateFlow<VehiclePosition?>(null)
    val selectedBus: StateFlow<VehiclePosition?> = _selectedBus.asStateFlow()

    // Currently selected route for schedule screen
    private val _selectedRouteId = MutableStateFlow<String>("")
    val selectedRouteId: StateFlow<String> = _selectedRouteId.asStateFlow()

    fun selectBus(vehicle: VehiclePosition) {
        _selectedBus.value = vehicle
    }

    fun selectRoute(routeId: String) {
        _selectedRouteId.value = routeId
    }

    fun getArrivalsForStop(stopId: String): List<ArrivalResult> {
        return repository.getArrivalsForStop(stopId)
    }

    fun getArrivalsForRoute(routeId: String): List<ArrivalResult> {
        return repository.getArrivalsForRoute(routeId)
    }

    // Returns "X sec ago" or "X min ago" string from lastUpdated timestamp
    fun getLastUpdatedText(): String {
        val lastMs = lastUpdated.value
        if (lastMs == 0L) return "Not yet updated"
        val secondsAgo = (System.currentTimeMillis() - lastMs) / 1000
        return when {
            secondsAgo < 60 -> "${secondsAgo}s ago"
            else -> "${secondsAgo / 60}m ago"
        }
    }
}
