package com.jeba.bloomingtontransit.data.repository

import android.util.Log
import com.jeba.bloomingtontransit.data.model.StopArrival
import com.jeba.bloomingtontransit.data.model.VehiclePosition
import com.jeba.bloomingtontransit.data.network.GtfsRealtimeParser
import com.jeba.bloomingtontransit.data.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransitRepository {

    private val TAG = "TRANSIT_REPO"
    private val api = NetworkModule.api

    // Live bus positions — updates every 10 seconds
    private val _vehiclePositions = MutableStateFlow<List<VehiclePosition>>(emptyList())
    val vehiclePositions: StateFlow<List<VehiclePosition>> = _vehiclePositions.asStateFlow()

    // All predicted arrivals from trip updates feed
    private val _stopArrivals = MutableStateFlow<List<StopArrival>>(emptyList())
    val stopArrivals: StateFlow<List<StopArrival>> = _stopArrivals.asStateFlow()

    // True while a fetch is in progress
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Unix millis of last successful fetch — use this to show "Updated X sec ago"
    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    fun startPolling(scope: CoroutineScope) {
        scope.launch {
            Log.d(TAG, "Polling started")
            while (true) {
                fetchAllData()
                delay(10_000L)
            }
        }
    }

    private suspend fun fetchAllData() {
        _isLoading.value = true
        Log.d(TAG, "Fetching... [${System.currentTimeMillis()}]")

        try { fetchVehiclePositions() } catch (e: Exception) {
            Log.e(TAG, "Vehicle fetch failed: ${e.message}")
        }

        try { fetchTripUpdates() } catch (e: Exception) {
            Log.e(TAG, "Trip update fetch failed: ${e.message}")
        }

        _lastUpdated.value = System.currentTimeMillis()
        _isLoading.value = false
        Log.d(TAG, "Done. Vehicles=${_vehiclePositions.value.size} Arrivals=${_stopArrivals.value.size}")
    }

    private suspend fun fetchVehiclePositions() {
        val bytes = api.getVehiclePositions().bytes()
        _vehiclePositions.value = GtfsRealtimeParser.parseVehiclePositions(bytes)
    }

    private suspend fun fetchTripUpdates() {
        val bytes = api.getTripUpdates().bytes()
        _stopArrivals.value = GtfsRealtimeParser.parseTripUpdates(bytes)
    }

    fun getArrivalsForStop(stopId: String): List<ArrivalResult> =
        ArrivalTimeCalculator.getArrivalsForStop(stopId, _stopArrivals.value)

    fun getArrivalsForRoute(routeId: String): List<ArrivalResult> =
        ArrivalTimeCalculator.getArrivalsForRoute(routeId, _stopArrivals.value)
}