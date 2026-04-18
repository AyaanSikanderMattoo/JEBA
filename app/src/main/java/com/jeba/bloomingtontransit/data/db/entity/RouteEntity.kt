
package com.jeba.bloomingtontransit.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val routeId: String,
    val shortName: String,
    val longName: String,
    val color: String
)