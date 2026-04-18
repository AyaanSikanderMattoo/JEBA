package com.jeba.bloomingtontransit.data.model

data class VehiclePosition(
    val vehicleId: String,
    val routeId: String,
    val tripId: String,
    val lat: Double,
    val lon: Double,
    val bearing: Float,
    val timestamp: Long
)