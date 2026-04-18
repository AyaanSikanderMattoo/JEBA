package com.jeba.bloomingtontransit.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stops")
data class StopEntity(
    @PrimaryKey val stopId: String,
    val stopName: String,
    val lat: Double,
    val lon: Double
)
