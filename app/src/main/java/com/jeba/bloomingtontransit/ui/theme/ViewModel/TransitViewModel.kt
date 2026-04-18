package com.jeba.bloomingtontransit.ui.theme.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jeba.bloomingtontransit.data.db.TransitDatabase
import com.jeba.bloomingtontransit.data.model.StopArrival
import com.jeba.bloomingtontransit.data.model.VehiclePosition
import com.jeba.bloomingtontransit.data.repository.ArrivalResult
import com.jeba.bloomingtontransit.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RepositoryProvider.transitRepository
    private val db = TransitDatabase.getInstance(application)

    val vehiclePositions: StateFlow<List<VehiclePosition>> = repository.vehiclePositions
    val stopArrivals: StateFlow<List<StopArrival>> = repository.stopArrivals
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val lastUpdated: StateFlow<Long> = repository.lastUpdated

    // tripId -> routeId, built once from Room trips table
    private val _tripToRouteCache = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _tripHeadsignCache = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _stopNameCache = MutableStateFlow<Map<String, String>>(emptyMap())

    // routeId -> shortName for display
    private val _routeNameMap = MutableStateFlow<Map<String, String>>(emptyMap())

    // Route IDs currently active, derived from arrivals resolved through cache
    private val _activeRouteIds = MutableStateFlow<List<String>>(emptyList())
    val activeRouteIds: StateFlow<List<String>> = _activeRouteIds.asStateFlow()

    private val _selectedBus = MutableStateFlow<VehiclePosition?>(null)
    val selectedBus: StateFlow<VehiclePosition?> = _selectedBus.asStateFlow()

    private val _selectedRouteId = MutableStateFlow("")
    val selectedRouteId: StateFlow<String> = _selectedRouteId.asStateFlow()

    init {
        viewModelScope.launch {
            // Load ALL trips from Room into memory as a map — fast lookup
            val allTrips = db.tripDao().getAllTrips()
            _tripToRouteCache.value = allTrips.associate { it.tripId to it.routeId }

            _tripHeadsignCache.value = allTrips.associate { it.tripId to it.tripHeadsign }

            val allStops = db.stopDao().getAllStops()
            _stopNameCache.value = allStops.associate { it.stopId to it.stopName }

            android.util.Log.d("STOP_CACHE", "Loaded ${_stopNameCache.value.size} stop names")

            // Load route short names
            val routes = db.routeDao().getAllRoutes()
            _routeNameMap.value = routes.associate { it.routeId to it.shortName }

            // Now watch arrivals and resolve which routes are active
            repository.stopArrivals.collect { arrivals ->
                val cache = _tripToRouteCache.value
                val routeIds = arrivals
                    .mapNotNull { cache[it.tripId] }
                    .distinct()
                    .sorted()
                _activeRouteIds.value = routeIds
            }
        }
    }

    fun getRouteDisplayName(routeId: String): String {
        val name = _routeNameMap.value[routeId]
        return if (!name.isNullOrEmpty()) "Route $name" else "Route $routeId"
    }

    fun selectBus(vehicle: VehiclePosition) { _selectedBus.value = vehicle }
    fun selectRoute(routeId: String) { _selectedRouteId.value = routeId }

    fun getArrivalsForStop(stopId: String): List<ArrivalResult> =
        repository.getArrivalsForStop(stopId)

    fun getArrivalsForRoute(routeId: String): List<ArrivalResult> {
        val nowSeconds = System.currentTimeMillis() / 1000L
        val cache = _tripToRouteCache.value

        val sampleStopId = repository.stopArrivals.value.firstOrNull()?.stopId
        val sampleCacheKey = _stopNameCache.value.keys.firstOrNull()
        android.util.Log.d("STOP_ID_CHECK", "Arrival stopId example: $sampleStopId")
        android.util.Log.d("STOP_ID_CHECK", "Cache key example: $sampleCacheKey")


        // Group arrivals by stopId, keep only the soonest arrival per stop
        return repository.stopArrivals.value
            .filter { arrival ->
                val resolvedRoute = cache[arrival.tripId] ?: return@filter false
                resolvedRoute == routeId
            }
            .groupBy { it.stopId }
            .mapNotNull { (stopId, arrivalsForStop) ->
                // Pick the soonest valid arrival for this stop
                val soonest = arrivalsForStop
                    .map { it to (it.arrivalTimeUnix - nowSeconds) }
                    .filter { (_, secondsAway) -> secondsAway in -60..7200 }
                    .minByOrNull { (_, secondsAway) -> secondsAway }
                    ?: return@mapNotNull null

                val (arrival, secondsAway) = soonest
                val resolvedRoute = cache[arrival.tripId] ?: return@mapNotNull null

                ArrivalResult(
                    stopId = _tripHeadsignCache.value[arrival.tripId] ?: "Route $routeId",
                    routeId = resolvedRoute,
                    tripId = arrival.tripId,
                    minutesAway = (secondsAway / 60).toInt(),
                    secondsAway = secondsAway,
                    displayText = when {
                        secondsAway < 60 -> "Arriving now"
                        secondsAway < 120 -> "1 min"
                        else -> "${secondsAway / 60} min"
                    }
                )
            }
            .sortedBy { it.secondsAway }
    }

    fun getLastUpdatedText(): String {
        val lastMs = lastUpdated.value
        if (lastMs == 0L) return "Not yet updated"
        val secondsAgo = (System.currentTimeMillis() - lastMs) / 1000
        return if (secondsAgo < 60) "${secondsAgo}s ago" else "${secondsAgo / 60}m ago"
    }
}