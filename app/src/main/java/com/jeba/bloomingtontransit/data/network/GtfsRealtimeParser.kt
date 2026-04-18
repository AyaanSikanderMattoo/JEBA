package com.jeba.bloomingtontransit.data.network

import android.util.Log
import com.google.transit.realtime.GtfsRealtime.FeedMessage
import com.jeba.bloomingtontransit.data.model.StopArrival
import com.jeba.bloomingtontransit.data.model.VehiclePosition

object GtfsRealtimeParser {

    private const val TAG = "GTFS_PARSER"

    fun parseVehiclePositions(bytes: ByteArray): List<VehiclePosition> {
        return try {
            val feed = FeedMessage.parseFrom(bytes)
            Log.d(TAG, "Parsing ${feed.entityCount} entities for vehicle positions")

            feed.entityList
                .filter { it.hasVehicle() }
                .mapNotNull { entity ->
                    val vehicle = entity.vehicle
                    if (!vehicle.hasPosition()) return@mapNotNull null

                    VehiclePosition(
                        vehicleId = vehicle.vehicle.id.ifEmpty { entity.id },
                        routeId = vehicle.trip.routeId,
                        tripId = vehicle.trip.tripId,
                        lat = vehicle.position.latitude.toDouble(),
                        lon = vehicle.position.longitude.toDouble(),
                        bearing = vehicle.position.bearing,
                        timestamp = vehicle.timestamp
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse vehicle positions: ${e.message}")
            emptyList()
        }
    }

    fun parseTripUpdates(bytes: ByteArray): List<StopArrival> {
        return try {
            val feed = FeedMessage.parseFrom(bytes)
            Log.d(TAG, "Parsing ${feed.entityCount} entities for trip updates")

            val arrivals = mutableListOf<StopArrival>()

            feed.entityList
                .filter { it.hasTripUpdate() }
                .forEach { entity ->
                    val tripUpdate = entity.tripUpdate
                    val routeId = tripUpdate.trip.routeId
                    val tripId = tripUpdate.trip.tripId

                    tripUpdate.stopTimeUpdateList.forEach { stopTimeUpdate ->
                        val arrivalTime = when {
                            stopTimeUpdate.hasArrival() && stopTimeUpdate.arrival.hasTime() ->
                                stopTimeUpdate.arrival.time
                            stopTimeUpdate.hasDeparture() && stopTimeUpdate.departure.hasTime() ->
                                stopTimeUpdate.departure.time
                            else -> return@forEach
                        }

                        arrivals.add(
                            StopArrival(
                                stopId = stopTimeUpdate.stopId,
                                routeId = routeId,
                                tripId = tripId,
                                arrivalTimeUnix = arrivalTime
                            )
                        )
                    }
                }

            arrivals
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse trip updates: ${e.message}")
            emptyList()
        }
    }
}