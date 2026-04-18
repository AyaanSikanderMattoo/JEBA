package com.jeba.bloomingtontransit.data.model

data class StopArrival(
    val stopId: String,
    val routeId: String,
    val tripId: String,
    val arrivalTimeUnix: Long
)