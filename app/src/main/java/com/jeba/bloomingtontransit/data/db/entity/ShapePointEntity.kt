package com.jeba.bloomingtontransit.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shape_points")
data class ShapePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shapeId: String,
    val lat: Double,
    val lon: Double,
    val sequence: Int
)