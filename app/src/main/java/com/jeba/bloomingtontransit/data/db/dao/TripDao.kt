package com.jeba.bloomingtontransit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jeba.bloomingtontransit.data.db.entity.TripEntity

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trips: List<TripEntity>)

    @Query("SELECT routeId FROM trips WHERE tripId = :tripId LIMIT 1")
    suspend fun getRouteIdForTrip(tripId: String): String?

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun count(): Int

    @Query("SELECT * FROM trips")
    suspend fun getAllTrips(): List<TripEntity>
}