package com.jeba.bloomingtontransit.data.repository

import com.jeba.bloomingtontransit.data.model.StopArrival

object ArrivalTimeCalculator {
    fun getArrivalsForStop(
        stopId: String,
        allArrivals: List<StopArrival>
    ): List<ArrivalResult> {
        val nowSeconds = System.currentTimeMillis() / 1000L

        return allArrivals
            .filter { it.stopId == stopId }
            .mapNotNull { arrival ->
                val secondsAway = arrival.arrivalTimeUnix - nowSeconds

                if (secondsAway < -30 || secondsAway > 5400) return@mapNotNull null

                ArrivalResult(
                    stopId = arrival.stopId,
                    routeId = arrival.routeId,
                    tripId = arrival.tripId,
                    minutesAway = (secondsAway / 60).toInt(),
                    secondsAway = secondsAway,
                    displayText = formatMinutes(secondsAway)
                )
            }
            .sortedBy { it.secondsAway }
    }

    /**
     * Get arrivals for a specific route across ALL stops.
     * Useful for populating the schedule screen by route.
     */
    fun getArrivalsForRoute(
        routeId: String,
        allArrivals: List<StopArrival>
    ): List<ArrivalResult> {
        val nowSeconds = System.currentTimeMillis() / 1000L

        return allArrivals
            .filter { it.tripId == routeId || it.routeId == routeId || it.routeId.isEmpty() }
            .mapNotNull { arrival ->
                val secondsAway = arrival.arrivalTimeUnix - nowSeconds
                if (secondsAway < -30 || secondsAway > 5400) return@mapNotNull null

                ArrivalResult(
                    stopId = arrival.stopId,
                    routeId = arrival.routeId,
                    tripId = arrival.tripId,
                    minutesAway = (secondsAway / 60).toInt(),
                    secondsAway = secondsAway,
                    displayText = formatMinutes(secondsAway)
                )
            }
            .sortedBy { it.secondsAway }
    }

    private fun formatMinutes(secondsAway: Long): String {
        return when {
            secondsAway < 60 -> "Arriving now"
            secondsAway < 120 -> "1 min"
            else -> "${secondsAway / 60} min"
        }
    }
}

data class ArrivalResult(
    val stopId: String,
    val routeId: String,
    val tripId: String,
    val minutesAway: Int,
    val secondsAway: Long,
    val displayText: String // "3 min", "Arriving now", etc.
)
